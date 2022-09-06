package com.learner.codereducer.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.forEachIndexed
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.learner.codereducer.R
import java.util.*

/** @author [Riz1Ahmed](https://fb.com/Riz1Ahmed)
 *
 * Date: 12/12/2021*/

fun Context.toast(msg: String, len: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, msg, len).show()

fun logD(msg: String, tag: String = "xyz:") = Log.d(tag, msg)

fun Activity.startActivity(cls: Class<*>) =
    Intent(this, cls).also {
        it.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(it)
    }


fun View.setViewReadyListener(listener: ViewReadyListener) {
    this.viewTreeObserver.addOnGlobalLayoutListener(object :
        ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            //Layout binding ready
            listener.onViewReady()
            this@setViewReadyListener.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
}

interface ViewReadyListener {
    fun onViewReady()
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.applyClickEffect() {
    alpha = .50f
    Handler(Looper.getMainLooper()).postDelayed({ alpha = 1f }, 200)
}

fun TextView.applyClickColorEffect(@ColorRes effectColor: Int, @ColorRes orgColor: Int) {
    //val tColor = orgColor ?: this.currentTextColor
    ContextCompat.getColor(this.context, effectColor).let { color ->
        setTextColor(color)
        for (drawable in this.compoundDrawables) drawable?.changeColor(color)
    }
    Handler(Looper.getMainLooper()).postDelayed({
        ContextCompat.getColor(this.context, orgColor).let { color ->
            setTextColor(color)
            for (drawable in this.compoundDrawables) drawable?.changeColor(color)
        }
    }, 100)
}

fun ImageView.applyClickColorEffect(@ColorRes effectColor: Int, @ColorRes orgColor: Int) {
    setColorFilter(ContextCompat.getColor(this.context, effectColor), PorterDuff.Mode.SRC_IN)
    Handler(Looper.getMainLooper()).postDelayed({
        setColorFilter(ContextCompat.getColor(this.context, orgColor), PorterDuff.Mode.SRC_IN)
    }, 100)
}

fun ImageView.setColor(@ColorRes color: Int) {
    setColorFilter(ContextCompat.getColor(this.context, color), PorterDuff.Mode.SRC_IN)
}

/**
 * Repeatedly call this method when user scrolled to end of this recyclerView.
 * @see layoutmanager must be GridLayoutManager of this recyclerview
 */
fun RecyclerView.onScrolledToEndListener(scrolled2End: () -> Unit) {
    val gridLayoutManager = layoutManager as GridLayoutManager
    addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (gridLayoutManager.findLastVisibleItemPosition() == gridLayoutManager.itemCount - 1)
                scrolled2End.invoke()
            super.onScrolled(recyclerView, dx, dy)
        }
    })
}

fun View.applyAnimation(context: Context, @AnimRes animResource: Int) =
    this.startAnimation(AnimationUtils.loadAnimation(context, animResource))

fun DialogFragment.show(activity: AppCompatActivity, tag: String = "tag") {
    show(activity.supportFragmentManager, tag)
}

//fun ImageView.setBitmap(assetPath: String) = Glide.with(this).load(assetPath).into(this)

fun Drawable.changeColor(color: Int) {
    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        this.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
    else this.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)*/
    //this.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    //LogD("Here color=$color")
    this.colorFilter =
        BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
}

fun Context.getDrawableImage(@DrawableRes DrawableResId: Int): Drawable? {
    return ContextCompat.getDrawable(this, DrawableResId)
}

fun Context.getColorFromRes(@ColorRes ResId: Int): Int {
    return ContextCompat.getColor(this, ResId)
}


fun connectViewPager2WithBtmNav(
    fm: FragmentManager, lc: Lifecycle,
    vp2: ViewPager2, bnv: BottomNavigationView, frs: List<Fragment>
) {
    class Vp2Adapter : FragmentStateAdapter(fm, lc) {
        override fun getItemCount() = frs.size
        override fun createFragment(position: Int) = frs[position]
    }
    vp2.adapter = Vp2Adapter()
    bnv.setOnItemSelectedListener {
        bnv.menu.forEachIndexed { index, item ->
            if (item.itemId == it.itemId) vp2.setCurrentItem(index).also {
                return@setOnItemSelectedListener true
            }
        }
        return@setOnItemSelectedListener false
    }
}

/**
 * @param pkgId Your app package ID/name
 * @param drawableName The name of resource drawable as string. ex: "ic frame" or "ic_frame".
 * Here the space( ) will replace with underscore(_) automatically.
 * @param defaultRes In case of not load [drawableName], then load this drawable.
 * This is not as string. It's pass like R.drawable.drawable_name
 */
@DrawableRes
fun Context.getDrawableResId(
    pkgId: String, drawableName: String,
    @DrawableRes defaultRes: Int = R.drawable.ic_launcher_foreground
): Int {
    //LogD("ic for: $drawableName")
    resources.getIdentifier(
        drawableName.lowercase(Locale.ROOT).replace(" ", "_"), "drawable", pkgId
    ).let { resId -> return if (resId != 0) resId else defaultRes }
}

fun Context.gtColor(@ColorRes colorResId: Int): Int {
    return ContextCompat.getColor(this, colorResId)
}

fun openUrl(context: Context, url: String) =
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

/**
 *This method is continuously runner. For example: if you want set a value to
 * view after view initialized (otherwise get null error, Then you have to
 * need to check the view is initialized or not.
 *
 * @param conditionCode This is your condition code. If return false then call
 * "falseCode" (2nd parameter) and then call again itself, Otherwise call
 * "trueCode". So until return true It's continuously call to [falseCode] and itself.
 * @param falseCode this method continuously call until the 1st method (conditionCode)
 * return true.
 * @param trueCode when [conditionCode] method return true then call this method.
 * And mind it, after call this method this main main not run again. Actually your
 * main code here.
 * @param delay The Delay time (in millisecond) to check [conditionCode] method.
 */
fun waitTillFalse(
    conditionCode: () -> Boolean, falseCode: () -> Unit, trueCode: () -> Unit,
    delay: Long = 100L
) {
    Handler(Looper.getMainLooper()).let { handler ->
        handler.postDelayed(object : Runnable {
            override fun run() {
                handler.removeCallbacks(this)
                if (!conditionCode.invoke()) {
                    falseCode.invoke()
                    handler.postDelayed(this, delay)
                } else trueCode.invoke()
            }
        }, 0)
    }
}

fun waitFor(delayInMillis: Long, runAfterDelay: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({ runAfterDelay.invoke() }, delayInMillis)
}

/**
 * @param continueCall Till hold the view continuously call this method with [delay].
 * @param holdUp when holdUp (Last time) will call this method
 * @param delay in Millisecond. how many time wait to repeat call [continueCall] method.
 * Default value is 50millisecond.
 */
fun View.setHoldingListener(continueCall: () -> Unit, holdUp: () -> Unit, delay: Long = 50) {
    setOnLongClickListener {
        waitTillFalse(
            conditionCode = { return@waitTillFalse !isPressed },
            falseCode = { continueCall.invoke() },
            trueCode = { holdUp.invoke() },
            delay = delay
        )
        return@setOnLongClickListener true
    }
}

fun Size.toPair() = Pair(this.width, this.height)
fun Size.set(width: Int, height: Int) = Size(width, height)

/**
 * This is short form of try-catch method.
 * try() = safeArea{} or the main method.
 * catch() = return value.
 * @return The Exception of catch. If return null then have ran without any error.
 */
inline fun safeArea(block: () -> Unit): Exception? {
    return try {
        block.invoke()
        null
    } catch (e: Exception) {
        e
    }
}

/**
 * @param intent if intent pass null than just pass [mimeType].
 * @param mimeType it's actually mime type. for example:
 * for image "image/ *" (without space)
 * for image "video/ *" (without space)
 * */
fun AppCompatActivity.openActivityResult(intent: Intent?, mimeType: String?, block: (Uri) -> Unit) {

    val mediaIntent = intent ?: Intent(Intent.ACTION_OPEN_DOCUMENT).also {
        it.addCategory(Intent.CATEGORY_OPENABLE)
        it.type = mimeType
        it.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
    }
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK && result.data != null && result.data!!.data != null) {
            block(result.data!!.data!!)
        }
    }.launch(mediaIntent)
}

val Activity?.isRunning get() = (this != null && !this.isFinishing && !this.isDestroyed)
val Context?.isActivity get() = (this != null && (this is Activity) && !this.isFinishing && !this.isDestroyed)

fun String?.ifNotNullAndEmpty(block: (String) -> Unit) {
    if (!isNullOrEmpty()) block(this)
}

/**
 * Return true if this [Context] is available.
 * Availability is defined as the following:
 * + [Context] is not null
 * + [Context] is not destroyed (tested with [FragmentActivity.isDestroyed] or [Activity.isDestroyed])
 */
fun Context?.isAvailable(): Boolean {
    if (this == null) return false
    else if (this !is Application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (this is FragmentActivity) return !this.isDestroyed
            else if (this is Activity) return !this.isDestroyed
        }
    }
    return true
}