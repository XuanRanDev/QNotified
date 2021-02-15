/*
 * QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2021 xenonhydride@gmail.com
 * https://github.com/ferredoxin/QNotified
 *
 * This software is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package me.nextalone.hook

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Looper
import android.view.View
import me.nextalone.util.*
import me.singleneuron.qn_kernel.data.hostInfo
import me.singleneuron.qn_kernel.data.requireMinQQVersion
import me.singleneuron.util.QQVersion
import nil.nadph.qnotified.SyncUtils
import nil.nadph.qnotified.config.ConfigManager
import nil.nadph.qnotified.hook.CommonDelayableHook
import nil.nadph.qnotified.ui.CustomDialog
import nil.nadph.qnotified.util.Toasts
import nil.nadph.qnotified.util.Utils

object SimplifyQQSettings : CommonDelayableHook("__NOT_USED__") {
    private const val na_simplify_qq_settings = "na_simplify_qq_settings"
    private val allItems = "手机号码|达人|安全|通知|记录|隐私|通用|辅助|免流量|关于".split("|")
    private const val defaultItems = "手机号码|达人|安全|通知|隐私|通用|辅助|关于"
    private var activeItems
        get() = ConfigManager.getDefaultConfig().getStringOrDefault(na_simplify_qq_settings, defaultItems).split("|").toMutableList()
        set(value) {
            var ret = ""
            for (item in value)
                ret += "|$item"
            putValue(na_simplify_qq_settings, if (ret.isEmpty()) ret else ret.substring(1))
        }

    fun simplifyQQSettingsClick() = View.OnClickListener {
        try {
            val cache = activeItems
            val ctx = it.context
            AlertDialog.Builder(ctx, CustomDialog.themeIdForDialog())
                .setTitle("选择要保留的条目")
                .setMultiChoiceItems(allItems.toTypedArray(), getBoolAry()) { _: DialogInterface, i: Int, _: Boolean ->
                    val item = allItems[i]
                    if (!cache.contains(item)) cache.add(item)
                    else cache.remove(item)
                }
                .setNegativeButton("取消", null)
                .setPositiveButton("确定") { _: DialogInterface, _: Int ->
                    activeItems = cache
                }
                .show()
        } catch (e: Exception) {
            Utils.log(e)
        }
    }
    

    override fun initOnce() = try {
        "Lcom/tencent/mobileqq/activity/QQSettingSettingActivity;->a(IIII)V".method.hookAfter(this) {
            val activity = it.thisObject as Activity
            val viewId: Int = it.args[0].toString().toInt()
            val strId: Int = it.args[1].toString().toInt()
            val view = activity.findViewById<View>(viewId)
            val str = activity.getString(strId)
            if (activeItems.all { string ->
                    string !in str
                }) {
                view.hide()
            }
        }
        if (!activeItems.contains("免流量"))
            "Lcom/tencent/mobileqq/activity/QQSettingSettingActivity;->a()V".method.replace(hookNull)
        true
    } catch (t: Throwable) {
        Utils.log(t)
        false
    }


    override fun isValid() = requireMinQQVersion(QQVersion.QQ_8_0_0)

    override fun isEnabled(): Boolean = activeItems.isNotEmpty()

    private fun getBoolAry(): BooleanArray {
        val ret = BooleanArray(allItems.size)
        for ((i, item) in allItems.withIndex()) {
            ret[i] = activeItems.contains(item)
        }
        return ret
    }

    private fun putValue(keyName: String, obj: Any) {
        try {
            val mgr = ConfigManager.getDefaultConfig()
            mgr.allConfig[keyName] = obj
            mgr.save()
        } catch (e: Exception) {
            Utils.log(e)
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toasts.error(hostInfo.application, e.toString() + "")
            } else {
                SyncUtils.post { Toasts.error(hostInfo.application, e.toString() + "") }
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {}
}