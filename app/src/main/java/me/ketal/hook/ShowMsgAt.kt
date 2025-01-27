/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2021 dmca@ioctl.cc
 * https://github.com/ferredoxin/QNotified
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by ferredoxin.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/ferredoxin/QNotified/blob/master/LICENSE.md>.
 */

package me.ketal.hook

import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import de.robv.android.xposed.XC_MethodHook
import me.ketal.dispacher.OnBubbleBuilder
import me.singleneuron.qn_kernel.data.MsgRecordData
import me.singleneuron.qn_kernel.data.isTim
import nil.nadph.qnotified.MainHook
import nil.nadph.qnotified.hook.CommonDelayableHook
import nil.nadph.qnotified.util.Utils
import xyz.nextalone.util.clazz
import xyz.nextalone.util.findHostView
import xyz.nextalone.util.get
import xyz.nextalone.util.invoke
import xyz.nextalone.util.method

object ShowMsgAt : CommonDelayableHook("Ketal_HideTroopLevel"), OnBubbleBuilder {

    override fun initOnce() = !isTim()

    override fun onGetView(
        rootView: ViewGroup,
        chatMessage: MsgRecordData,
        param: XC_MethodHook.MethodHookParam
    ) {
        if (!isEnabled || 1 != chatMessage.isTroop) return
        val textMsgType = "com.tencent.mobileqq.data.MessageForText".clazz!!
        val extStr = chatMessage.msgRecord.invoke(
            "getExtInfoFromExtStr",
            "troop_at_info_list", String::class.java
        ) ?: return
        if ("" == extStr) return
        val atList = (textMsgType.method("getTroopMemberInfoFromExtrJson")
            ?.invoke(null, extStr) ?: return) as List<*>
        when (val content = rootView.findHostView<View>("chat_item_content_layout")) {
            is TextView -> {
                copeAtInfo(content, atList)
            }
            is ViewGroup -> {
                content.forEach {
                    if (it is TextView)
                        copeAtInfo(it, atList)
                }
            }
            else -> {
                Utils.logd("暂不支持的控件类型--->$content")
                return
            }
        }
    }

    private fun copeAtInfo(textView: TextView, atList: List<*>) {
        val spannableString = SpannableString(textView.text)
        atList.forEach {
            val uin = it.get("uin") as Long
            val start = (it.get("startPos") as Short).toInt()
            val length = it.get("textLen") as Short
            if (spannableString[start] == '@')
                spannableString.setSpan(ProfileCardSpan(uin), start, start + length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        textView.text = spannableString
        textView.movementMethod = LinkMovementMethod.getInstance()
    }
}

class ProfileCardSpan(val qq: Long) : ClickableSpan() {
    override fun onClick(v: View) {
        MainHook.openProfileCard(v.context, qq)
    }
}
