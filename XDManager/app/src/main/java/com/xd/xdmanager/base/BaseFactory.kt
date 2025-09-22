package com.xd.xdmanager.base

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.wug.framew.util.Helper
import com.xd.xdmanager.R
import com.xd.xdmanager.frame.InterFieldMethod
import org.jetbrains.anko.internals.AnkoInternals
import java.io.*
import java.util.*

inline fun <reified T : Activity> Context.wStartActivity(vararg params: Pair<String, Any?>) = Helper.internalStartActivity(this, T::class.java, params)

inline fun <reified T : Activity> Activity.wStartActivityForResult(
    requestCode: Int, vararg params: Pair<String, Any?>
) = Helper.internalStartActivityForResult(this, T::class.java, requestCode, params)

inline fun <reified T : Service> Context.wStartService(vararg params: Pair<String, Any?>) = Helper.internalStartService(this, T::class.java, params)

inline fun <reified T : Service> Context.wStopService(vararg params: Pair<String, Any?>) = Helper.internalStopService(this, T::class.java, params)

inline fun <reified T : Activity> Fragment.wStartActivity(vararg params: Pair<String, Any?>) = Helper.internalStartActivity(activity!!, T::class.java, params)

inline fun <reified T : Any> Context.intentFor(vararg params: Pair<String, Any?>): Intent = AnkoInternals.createIntent(this, T::class.java, params)

fun <T> Intent.get(key: String): T? {
    try {
        val extras = InterFieldMethod.mExtras.get(this) as Bundle
        InterFieldMethod.unparcel.invoke(extras)
        val map = InterFieldMethod.mMap.get(extras) as Map<String, Any>
        return map[key] as T
    } catch (e: Exception) {
    }
    return null
}

fun <T> data2Model(data: String?, mclass: Class<T>): T? {
    return Gson().fromJson(data, mclass)
}

fun <T> data2List(json: String?, cls: Class<T>): List<T> {
    val list = ArrayList<T>()
    try {
        val array = JsonParser().parse(json).asJsonArray
        for (element in array) {
            list.add(Gson().fromJson(element, cls))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return list
}

fun putShareP(context: Context, key: String, value: String) {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    sp.edit {
        putString(key, value)
    }
}

fun putShareP(context: Context, key: String, value: Int) {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    sp.edit {
        putInt(key, value)
    }
}

fun putShareP(context: Context, key: String, value: Boolean) {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    sp.edit {
        putBoolean(key, value)
    }
}

fun putShareP(context: Context, key: String, value: Float) {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    sp.edit {
        putFloat(key, value)
    }
}

fun getShareP(context: Context, key: String): String? {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    return sp.getString(key, "")
}

fun getShareP(context: Context, key: String, defaultInt: Int): Int {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    return sp.getInt(key, defaultInt)
}

fun getShareP(context: Context, key: String, defaultBoolean: Boolean): Boolean {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    return sp.getBoolean(key, defaultBoolean)
}

fun getShareP(context: Context, key: String, defaultFloat: Float): Float {
    val sp = context.getSharedPreferences(
        context.getString(
            R.string.app_name
        ), Context.MODE_PRIVATE
    )
    return sp.getFloat(key, defaultFloat)
}

/**
 * 保存对象
 *
 * @param context 上下文
 * @param key     键
 * @param obj     要保存的对象（Serializable的子类）
 * @param <T>     泛型定义
 */
fun <T : Serializable?> putShareP(context: Context, key: String, obj: T) {
    try {
        putObj(context, key, obj)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 获取对象
 *
 * @param context 上下文
 * @param key     键
 * @param <T>     指定泛型
 * @return 泛型对象
</T> */
fun <T : Serializable?> getShareP(context: Context, key: String): T? {
    try {
        return getObj(context, key) as T?
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

/*****************************************************************************************************************************************************************/

fun <T> Any?.notNull(f: () -> T, t: () -> T): T {
    return if (this != null) f() else t()
}

fun Context.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

fun Context.px2dp(px: Int): Int {
    val scale = resources.displayMetrics.density
    return (px / scale + 0.5f).toInt()
}

fun View.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

fun View.px2dp(px: Int): Int {
    val scale = resources.displayMetrics.density
    return (px / scale + 0.5f).toInt()
}

/*****************************************************************************************************************************************************************/

/**存储对象 */
@Throws(IOException::class)
private fun putObj(context: Context, key: String, obj: Any?) {
    if (obj == null) return //判断对象是否为空
    val baos = ByteArrayOutputStream()
    var oos: ObjectOutputStream? = null
    oos = ObjectOutputStream(baos)
    oos.writeObject(obj)
    // 将对象放到OutputStream中
    // 将对象转换成byte数组，并将其进行base64编码
    val objectStr = String(Base64.encode(baos.toByteArray(), Base64.DEFAULT))
    baos.close()
    oos.close()
    putShareP(context, key, objectStr)
}

/**获取对象 */
@Throws(IOException::class, ClassNotFoundException::class)
private fun getObj(context: Context, key: String): Any? {
    val wordBase64 = getShareP(context, key)
    // 将base64格式字符串还原成byte数组
    if (TextUtils.isEmpty(wordBase64)) { //不可少，否则在下面会报java.io.StreamCorruptedException
        return null
    }
    val objBytes: ByteArray = Base64.decode(wordBase64?.toByteArray(), Base64.DEFAULT)
    val bais = ByteArrayInputStream(objBytes)
    val ois = ObjectInputStream(bais)
    // 将byte数组转换成product对象
    val obj: Any = ois.readObject()
    bais.close()
    ois.close()
    return obj
}