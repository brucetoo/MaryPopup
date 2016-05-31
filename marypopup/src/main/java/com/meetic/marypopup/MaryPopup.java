package com.meetic.marypopup;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.meetic.dragueur.Direction;
import com.meetic.dragueur.DraggableView;
import com.meetic.dragueur.ExitViewAnimator;
import com.meetic.dragueur.ReturnOriginViewAnimator;
import com.meetic.marypopup.DurX.Listeners;

import java.lang.ref.WeakReference;

/**
 * Created by florentchampigny on 14/04/2016.
 */
public class MaryPopup implements View.OnClickListener {

    final Activity activity;
    final ViewGroup activityView;
    final View actionBarView;

    View viewOrigin;

    @Nullable
    View blackOverlay;
    int blackOverlayColor = Color.parseColor("#CC333333");

    @Nullable
    ViewGroup popupView;
    @Nullable
    ViewGroup popupViewContent;

    Integer popupBackgroundColor;

    float differenceScaleX;
    float differenceTranslationX;
    float differenceScaleY;
    float differenceTranslationY;

    View contentLayout;
    int height = -1;
    int width = -1;
    boolean cancellable;

    long openDuration = 200;
    long closeDuration = 200;

    boolean center = false;
    boolean draggable = false;
    boolean scaleDownDragging = false;
    boolean fadeOutDragging = false;
    boolean inlineMove = true;
    boolean shadow = true;
    boolean scaleDownCloseOnDrag = false;
    boolean scaleDownCloseOnClick = true;

    boolean handleClick = false;
    boolean isAnimating = false;

    MaryPopup(Activity activity) {
        this.activity = activity;
        //activity 最顶层的view
        this.activityView = (ViewGroup) activity.findViewById(android.R.id.content);
        //actionbar 容器view
        this.actionBarView = activityView.findViewById(R.id.action_bar_container);
    }

    public static MaryPopup with(Activity activity) {
        return new MaryPopup(activity);
    }

    public MaryPopup from(View viewOrigin) {
        this.viewOrigin = viewOrigin;
        return this;
    }

    /**
     * contentView
     * @param contentLayoutId contentId 或者 直接使用contentView
     * @return
     */
    public MaryPopup content(int contentLayoutId) {
        View contentView = LayoutInflater.from(activity).inflate(contentLayoutId, popupView, false);
        content(contentView);
        return this;
    }

    public MaryPopup content(View view) {
        this.contentLayout = view;
        return this;
    }

    public MaryPopup width(int width) {
        this.width = width;
        return this;
    }

    public MaryPopup height(int height) {
        this.height = height;
        return this;
    }

    public MaryPopup blackOverlayColor(int blackOverlayColor) {
        this.blackOverlayColor = blackOverlayColor;
        return this;
    }

    public MaryPopup backgroundColor(Integer popupBackgroundColor) {
        this.popupBackgroundColor = popupBackgroundColor;
        return this;
    }

    public MaryPopup cancellable(boolean cancellable) {
        this.cancellable = cancellable;
        return this;
    }

    public MaryPopup draggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }

    public MaryPopup inlineMove(boolean inlineMove) {
        this.inlineMove = inlineMove;
        return this;
    }

    public MaryPopup fadeOutDragging(boolean fadeOutDragging) {
        this.fadeOutDragging = fadeOutDragging;
        return this;
    }

    public MaryPopup scaleDownDragging(boolean scaleDownDragging) {
        this.scaleDownDragging = scaleDownDragging;
        return this;
    }

    public MaryPopup openDuration(long duration) {
        this.openDuration = duration;
        return this;
    }

    public MaryPopup closeDuration(long duration) {
        this.closeDuration = duration;
        return this;
    }

    public MaryPopup shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public MaryPopup scaleDownCloseOnDrag(boolean scaleDownCloseOnDrag) {
        this.scaleDownCloseOnDrag = scaleDownCloseOnDrag;
        return this;
    }

    public MaryPopup scaleDownCloseOnClick(boolean scaleDownCloseOnClick) {
        this.scaleDownCloseOnClick = scaleDownCloseOnClick;
        return this;
    }

    public void show() {
        if (blackOverlay == null) {
            {
                handleClick = false;
                //初始化遮罩层view
                blackOverlay = new View(activity);
                //设置遮罩层颜色
                blackOverlay.setBackgroundColor(blackOverlayColor);
                //遮罩层add到父布局
                activityView.addView(blackOverlay, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                //遮罩层执行alpha动画
                DurX.putOn(blackOverlay)
                    .animate()
                    .alpha(0f, 1f);
                //处理遮罩层的点击事件
                blackOverlay.setOnClickListener(this);
            }
            {
                //是否是可拖拽的view
                if (draggable) {
                    //初始化可拖拽的view
                    popupView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.popup_layout_draggable, activityView, false);
                } else {
                    //初始化不可拖拽的view
                    popupView = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.popup_layout, activityView, false);
                }
                if (popupView != null) {
                    if (width >= 0) {//already set width
                        ViewGroup.LayoutParams layoutParams = popupView.getLayoutParams();
                        layoutParams.width = width;
                        popupView.setLayoutParams(layoutParams);
                    }
                    if (height >= 0) {//如果已经设置高度
                        ViewGroup.LayoutParams layoutParams = popupView.getLayoutParams();
                        layoutParams.height = height;
                        popupView.setLayoutParams(layoutParams);
                    }

                    //单独处理可拖拽view的参数设置
                    //TODO 分析DraggableView的代码
                    if(popupView instanceof DraggableView) {
                        DraggableView draggableView = (DraggableView) popupView;

                        draggableView.setDraggable(false);
                        draggableView.setInlineMove(inlineMove);
                        draggableView.setVertical(true);
                        draggableView.setListenVelocity(false);
                        draggableView.setMaxDragPercentageY(0.35f);

                        if (scaleDownCloseOnDrag) {
                            draggableView.setViewAnimator(new ReturnOriginViewAnimator() {
                                @Override
                                public boolean animateExit(@NonNull DraggableView draggableView, Direction direction, int duration) {
                                    close(true);
                                    return true;
                                }
                            });
                        } else {
                            draggableView.setViewAnimator(new ExitViewAnimator() {
                            });
                        }
                        draggableView.setDragListener(new DraggableViewListener(this));
                    }
                    //根布局点击没做处理
                    popupView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });
                    //弹出层的包裹布局
                    popupViewContent = (ViewGroup) popupView.findViewById(R.id.content);
                    if (!shadow) {//无阴影 -- 直接设置背景颜色
                        popupView.setBackgroundColor(popupBackgroundColor);
                    } else {//有阴影 -- 用ViewCompat包中的elevation兼容方法设置阴影
                        if (popupBackgroundColor != null && popupViewContent != null) {
                            popupViewContent.setBackgroundColor(popupBackgroundColor);
                        }
                        ViewCompat.setElevation(popupView, 6);
                    }
                    //contentLayout 是指实际需要添加到 popupViewContent的真实布局
                    if (contentLayout != null && popupViewContent != null) {
                        //此处是为了房子 contentLayout 已经添加到了一个父布局，再次加入popupViewContent中报异常
                        if (contentLayout.getParent() != null) {
                            //如果有父节点,将contentLayout从父节点中移除
                            ((ViewGroup) contentLayout.getParent()).removeView(contentLayout);
                        }
                        //添加到popupViewContent布局中
                        popupViewContent.addView(contentLayout);
                    }

                    //执行动画 将popupView从点击的位置渐隐变大到最终的布局中
                    DurX.putOn(popupView)
                        .pivotX(0f)
                        .pivotY(0f)//中心点设置在左上角点
                        .invisible()//设置不可见
                        .waitForSize(new Listeners.Size() {//此处是为了保证该view已经被绘制成功
                            @Override
                            public void onSize(DurX durX) {
                                //viewOrigin是原始点击的view,及动画开始的地方
                                if (viewOrigin != null) {
                                    //宽度的缩放比
                                    differenceScaleX = viewOrigin.getWidth() * 1.0f / popupView.getWidth();
                                    //高度的缩放比
                                    differenceScaleY = viewOrigin.getHeight() * 1.0f / popupView.getHeight();

                                    float translationX;
                                    float translationY;

                                    if (center) {//居中放大处理 效果貌似不佳 TODO 待验证
                                        differenceTranslationX = getX(viewOrigin);
                                        differenceTranslationY = getY(viewOrigin);
                                        translationX = activityView.getWidth() / 2 - popupView.getWidth() / 2;
                                        translationY = activityView.getHeight() / 2 - popupView.getHeight() / 2;
                                    } else { //计算popupView从viewOrigin转换过来的位移参数
                                        differenceTranslationX = getX(viewOrigin) - getX(popupView);
                                        differenceTranslationY = getY(viewOrigin) - getY(popupView);
                                        //从viewOrigin左边沿移动到popupView中心的长度
                                        translationX = getX(viewOrigin) - (popupView.getWidth() - viewOrigin.getWidth()) / 2f;
                                        translationY = getY(viewOrigin) - getStatusBarHeight();
                                    }

                                    DurX.putOn(popupView)
                                        .translationX(differenceTranslationX)
                                        .translationY(differenceTranslationY)
                                        .visible()//设置到viewOrigin的位置 并且visible

                                        //先执行背景Container的动画
                                        .animate()//初始化动画类 ViewPropertyAnimatorCompat
                                        .scaleX(differenceScaleX, 1f)
                                        .scaleY(differenceScaleY, 1f)//动画执行从viewOrigin到popupView的缩放
                                        .translationX(differenceTranslationX, translationX)
                                        .translationY(differenceTranslationY, translationY)//动画执行从viewOrigin到popupView的位移
                                        .duration(openDuration)//开启执行的时间
                                        .end(new Listeners.End() {
                                            @Override
                                            public void onEnd() {
                                                if (popupView instanceof DraggableView) {
                                                    DraggableView draggableView = (DraggableView) popupView;
                                                    draggableView.initOriginalViewPositions();
                                                    draggableView.setDraggable(draggable);
                                                }
                                                blackOverlay.setClickable(true);
                                                handleClick = true;
                                            }
                                        })
                                        .pullOut()//获取DurX 对象

                                        //在执行popupView中content内容的动画
                                        .andPutOn(popupViewContent)//重新添加需要执行属性变化的view
                                        .visible()
                                        .animate()
                                        .startDelay(openDuration - 100)//延迟100ms执行
                                        .alpha(0f, 1f);
                                }
                            }
                        });
                    //将popupView 添加到 R.id.content 中
                    activityView.addView(popupView);
                }
            }
        }
    }

    /**
     * 只有当遮罩层不为空是才能点击关闭
     * @return
     */
    public boolean canClose() {
        return blackOverlay != null;
    }

    /**
     * 检测是否开启也是根据是否有遮罩层来判断的
     * @return
     */
    public boolean isOpened() {
        return blackOverlay != null;
    }

    /**
     * 关闭该弹出层
     * @param withScaleDown 是否有缩放动画
     * @return
     */
    public boolean close(final boolean withScaleDown) {
        if (blackOverlay != null) {

            handleClick = false;

            //执行完动画后的状态清理
            final Listeners.End clearListener = new Listeners.End() {
                @Override
                public void onEnd() {
                    //动画执行完后 移除遮罩层
                    activityView.removeView(blackOverlay);
                    //制空 利于回收
                    blackOverlay = null;
                    //移除popupView
                    activityView.removeView(popupView);
                    popupView = null;
                    isAnimating = false;
                }
            };

            isAnimating = true;
            //需要执行缩放动画 //TODO 分析以下代码
            if (withScaleDown) {

                float scaleX = viewOrigin.getWidth() * 1.0f / (popupView.getWidth() * ViewCompat.getScaleX(popupView));
                float scaleY = viewOrigin.getHeight() * 1.0f / (popupView.getHeight() * ViewCompat.getScaleY(popupView));

                float translationX;
                float translationY;

                if (center) {
                    translationX = 0;
                    translationY = 0;
                } else {
                    translationX = ViewCompat.getTranslationX(popupView);
                    translationY = ViewCompat.getTranslationY(popupView) - getStatusBarHeight();
                }

                float xViewOrigin = getX(viewOrigin);
                float yViewOrigin = getY(viewOrigin);

                float xPopupView = getX(popupView);
                float yPopupView = getY(popupView) - getStatusBarHeight();

                float tx = 0;
                if (xViewOrigin < xPopupView) {
                    tx = xPopupView - xViewOrigin;
                } else { // xViewOrigin > xPopupView
                    tx = xViewOrigin - xPopupView;
                }

                if (center) {
                    tx *= (1f - scaleX);
                }
                translationX += tx;

                float ty = 0;
                if (yViewOrigin < yPopupView) {
                    ty = yPopupView - yViewOrigin;
                } else { // yViewOrigin > yPopupView
                    ty = yViewOrigin - yPopupView;
                }

                if (center) {
                    ty *= (1f - scaleY);
                }
                translationY += ty;

                DurX.putOn(popupViewContent)
                    .animate()
                    .alpha(0f)
                    .duration(closeDuration)

                    .andAnimate(popupView)
                    .scaleX(scaleX)
                    .scaleY(scaleY)
                    .alpha(0f)
                    .translationX(translationX)
                    .translationY(translationY)
                    .duration(closeDuration)

                    .andAnimate(blackOverlay)
                    .alpha(0f)
                    .duration(closeDuration)
                    .end(clearListener)
                ;
            } else {
                DurX.putOn(blackOverlay)
                    .animate()
                    .alpha(0)
                    .duration(closeDuration)

                    .thenAnimate(popupViewContent)
                    .alpha(0)
                    .duration(closeDuration)
                    .end(clearListener);
            }

            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (cancellable && handleClick) {
            close(scaleDownCloseOnClick);
        }
    }

    public MaryPopup center(boolean center) {
        this.center = center;
        return this;
    }

    /**
     * 获取statusBar的高度其实很简单
     * 值需要获取到 android.R.id.content view在
     * 全局中的top值 = 状态栏高度
     * @return
     */
    public float getStatusBarHeight() {
        return getY(activityView);
    }

    /**
     * 获取一个view在屏幕中的y坐标
     * @param view
     * @return
     */
    float getY(View view) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        float y = rect.top;
        if (y <= 0) {
            y = ViewCompat.getY(view);
        }
        return y;
    }

    /**
     * 获取一个view在屏蔽中的x坐标
     * @param view
     * @return
     */
    float getX(View view) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        float x = rect.left;
        if (x <= 0) {
            x = ViewCompat.getX(view);
        }
        return x;
    }

    static class DraggableViewListener extends DraggableView.DraggableViewListenerAdapter {

        WeakReference<MaryPopup> reference;

        public DraggableViewListener(MaryPopup popup) {
            this.reference = new WeakReference<>(popup);
        }

        @Override
        public void onDrag(DraggableView draggableView, float percentX, float percentY) {
            super.onDrag(draggableView, percentX, percentY);

            MaryPopup popup = reference.get();
            if (popup != null && !popup.isAnimating) {
                float percent = 1f - Math.abs(percentY);

                if (popup.fadeOutDragging) {
                    DurX.putOn(popup.popupView)
                        .alpha(percent);
                }

                if (popup.scaleDownDragging) {
                    float scale = Math.max(0.75f, percent);
                    DurX.putOn(popup.popupView)
                        .pivotX(0.5f)
                        .scale(scale);
                }
            }
        }

        @Override
        public void onDraggedStarted(DraggableView draggableView, Direction direction) {
            MaryPopup popup = reference.get();
            if (popup != null) {
                popup.close(false);
            }
        }
    }
}
