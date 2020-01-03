package com.erolc.estatusbar


import android.annotation.TargetApi
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.DrawableRes
import android.util.TypedValue
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LOW_PROFILE
import android.view.Window.ID_ANDROID_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.Fragment

const val STATUS_BAR = "statusBar"

var statusBarDebug: Boolean
    get() = DEBUG
    set(value) {
        DEBUG = value
    }
/**
 * 状态栏高度
 */
val Activity.statusBarHeight
    get() = run {
        val identifier = resources.getIdentifier("status_bar_height", "dimen", "android")
        val i = if (identifier > 0) resources.getDimensionPixelSize(identifier) else 0
        log("statusBarHeight is $i")
        i
    }
/**
 * 状态栏的背景颜色
 */
var Activity.statusBarColor
    get() = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            log("get statusBar color with System")
            window.statusBarColor //系统状态栏的背景颜色
        } else {
            log("get statusBar color with customize")
            val background = getStatusBarView().background//自定义的状态栏背景颜色
            if (background is ColorDrawable) background.color else -1//如果这个背景不是颜色，（自定义状态栏的背景可以是drawable），则返回-1
        }
    }
    set(value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = value//设置系统状态栏背景颜色
            log("set the system's statusBarColor to $value color")
        } else {
            getStatusBarView().setBackgroundColor(value)
            log("set the customize's statusBarColor to $value color")
        }
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or SYSTEM_UI_FLAG_LOW_PROFILE //将状态栏中不必要的图标隐藏掉
        setStatusBarTextColor(value == Color.WHITE)//设置自定义状态栏的字体颜色
    }
/**
 * 状态栏是否显示
 */
val Activity.isShowStatusBar get() = window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN == 0

/**
 * 内容布局
 */
private val Activity.contentView
    get() = window.decorView.findViewById<FrameLayout>(
        ID_ANDROID_CONTENT
    )

/**
 * 系统标题栏高度
 */
val Activity.actionBarHeight
    get() = run {
        val value = TypedValue()
        val height =
            if (theme.resolveAttribute(android.R.attr.actionBarSize, value, true))//判断标题栏是否存在
                TypedValue.complexToDimensionPixelSize(value.data, resources.displayMetrics)
            else
                0
        log("the statusBar height is $height")
        height
    }

/**
 * 状态栏字体颜色是否为暗色
 */
val Activity.statusBarTextColorIsDark
    get() = run {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0
        } else {
            loge("SDK_INT must be more than M")
            false
        }
    }

/**
 * 状态栏字体的颜色
 * @param isDark 由于状态栏字体颜色只有明和暗两种，true为明，false为暗
 * @param isReserved 由于状态栏字体颜色的设置会与之前的其他状态会有冲突，所以该参数是为是否保留之前的状态，true保留，false 不保留
 * 目标api是M以上
 */
@TargetApi(Build.VERSION_CODES.M)
fun Activity.setStatusBarTextColor(isDark: Boolean, isReserved: Boolean = true) {
    val systemUiVisibility = window.decorView.systemUiVisibility
    var option = if (isDark) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else View.SYSTEM_UI_FLAG_VISIBLE
    option = if (isReserved) option or systemUiVisibility else option
    window.decorView.systemUiVisibility = option
}

/**
 * 隐藏状态栏，手动在顶部下滑可重新显示，之后还会自动隐藏，
 */
fun Activity.hideStatusBar() {
    statusBar?.visibility = View.GONE
    window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
    if (isCustomizeStatusBar())
        updateLayout()
}

/**
 * 展示状态栏，与hideStatusBar()方法是一套的，
 */
fun Activity.showStatusBar() {
    statusBar?.visibility = View.VISIBLE
    window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
    )
    if (isCustomizeStatusBar())
        updateLayout()
}

/**
 * @param drawableRes 状态栏背景的drawable文件资源
 * @param isDark 状态栏字体颜色是否是黑色
 */
fun Activity.setStatusBarBackground(
    @DrawableRes drawableRes: Int,
    isDark: Boolean = true
) {
    getStatusBarView().setBackgroundResource(drawableRes)
    setStatusBarTextColor(isDark)
}

/**
 * 设置状态栏背景
 */
fun Activity.setStatusBarBackground(drawable: Drawable, isDark: Boolean = true) {
    getStatusBarView().background = drawable
    setStatusBarTextColor(isDark)
}

fun Activity.getStatusBarBackground(): Drawable {
    return getStatusBarView().background
}

/**
 * 自定义StatusBar
 */
private fun Activity.getStatusBarView(): View {
    val view = statusBar ?: View(this)//获取自定义的状态栏，如果没有则新建
    view.tag = STATUS_BAR//取得其中的tag
    immersive()//隐藏系统状态栏
    updateLayout()//更新布局
    contentView.addView(view)//将自定义状态栏添加到内容布局中，由于内容布局是frameLayout，那么会在顶部
    val layoutParams = view.layoutParams as FrameLayout.LayoutParams
    layoutParams.height = statusBarHeight//设置自定义状态栏高度
    loge("the contentView.paddingTop is ${contentView.paddingTop}")
    layoutParams.topMargin = -contentView.paddingTop
    view.layoutParams = layoutParams//使用布局参数，使其生效
    return view//这里就得到最终的自定义状态栏
}

/**
 * 是否为自定义的状态栏
 */
private fun Activity.isCustomizeStatusBar(): Boolean {
    return contentView.findViewWithTag<View>(STATUS_BAR) != null//根据tag查看自定义状态栏是否存在
}

/**
 * 让系统的状态栏背景消失，计算一些数值
 * 系统标题栏和内容布局无关，所以你需要重新计算，否则内容布局会被标题栏遮盖
 */
private fun Activity.updateLayout(defaultTop: Int = -1) {
    val findViewById =
        window.decorView.findViewById<View>(R.id.action_mode_bar_stub)//这个view在标题栏存在时，不存在，
    var paddingTop =//计算并得到标题栏存在或者不存在的时候的上内边距
        if (findViewById == null) {//表明标题栏存在
            actionBarHeight + if (isShowStatusBar) statusBarHeight else 0//当标题栏存在的时候，上内边距就需要包括标题栏的高度，以及自定义的状态栏的高度，当然前提是这个状态栏存在
        } else {
            if (isShowStatusBar) statusBarHeight else 0//当标题栏不存在的时候，只需要考虑状态栏的高度
        }
    if (defaultTop != -1) {
        paddingTop = defaultTop//如果有默认高度，那么就使用默认高度
    }
    contentView.clipToPadding = false//让子view可以扩展到padding中去
    contentView.setPadding(0, paddingTop, 0, 0)//设置内容的padding
}

/**
 * 获得自定义状态栏
 */
private val Activity.statusBar: View? get() = contentView.findViewWithTag(STATUS_BAR)

/**
 * 状态栏背景消失，内容层渗透到状态栏的区域里。
 */
fun Activity.immersive() {
    log("immersive")
    window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    val findViewWithTag = statusBar
    if (findViewWithTag != null) {
        contentView.removeView(findViewWithTag)
        updateLayout(0)
    }
    statusBarColor = Color.TRANSPARENT
}

/*----------------------------fragment------------------------------------*/

fun Fragment.setStatusBarBackground(@DrawableRes res: Int, isDark: Boolean = true) {
    requireActivity().setStatusBarBackground(res, isDark)
}

fun Fragment.setStatusBarBackground(res: Drawable, isDark: Boolean = true) {
    requireActivity().setStatusBarBackground(res, isDark)
}

fun Fragment.showStatusBar() {
    requireActivity().showStatusBar()
}

fun Fragment.hideStatusBar() {
    requireActivity().hideStatusBar()
}

fun Fragment.immersive() {
    requireActivity().immersive()
}

/**
 * 状态栏高度
 */
val Fragment.statusBarHeight
    get() = run {
        requireActivity().statusBarHeight
    }
/**
 * 状态栏的背景颜色
 */
var Fragment.statusBarColor
    get() = run {
        requireActivity().statusBarColor
    }
    set(value) {
        requireActivity().statusBarColor = value
    }

/**
 * 状态栏是否显示
 */
val Fragment.isShowStatusBar
    get() = requireActivity().isShowStatusBar

val Fragment.statusBarTextColorIsDark get() = requireActivity().statusBarTextColorIsDark