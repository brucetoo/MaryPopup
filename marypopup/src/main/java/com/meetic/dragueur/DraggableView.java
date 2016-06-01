package com.meetic.dragueur;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class DraggableView extends FrameLayout {

    public static final int DEFAULT_EXIT_DURATION = 150;

    //原始view的x,y坐标
    public float motionXOrigin;
    public float motionYOrigin;

    public float parentWidth;
    public float parentHeight;

    protected float oldPercentX = 0;
    protected float oldPercentY = 0;

    float maxDragPercentageY = 0.75f;
    float maxDragPercentageX = 0.75f;//最大拖拽比例

    boolean listenVelocity = true;//监听速率?
    boolean draggable = true;
    boolean inlineMove = false;
    boolean vertical = false;//监听垂直滚动
    boolean rotationEnabled;
    float rotationValue;
    boolean animating;//if draggable view already animating
    float minVelocity;
    DraggableViewListener dragListener;
    GestureDetectorCompat detector;
    @Nullable
    ViewAnimator<DraggableView> viewAnimator;

    //ReturnOriginViewAnimator will reset the view to this positions
    float originalViewX = 0;
    float originalViewY = 0;

    public DraggableView(Context context) {
        this(context, null);
    }

    public DraggableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public void setViewAnimator(@Nullable ViewAnimator viewAnimator) {
        this.viewAnimator = viewAnimator;
    }

    @Nullable
    public ViewAnimator<DraggableView> getViewAnimator() {
        return viewAnimator;
    }

    /**
     * When left-right top-bottom animate exit not be handled,
     * We need animate to origin location
     * @param duration
     */
    public void animateToOrigin(int duration){
        if(viewAnimator != null){
            viewAnimator.animateToOrigin(this, duration);
        }
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean isListenVelocity() {
        return listenVelocity;
    }

    public void setListenVelocity(boolean listenVelocity) {
        this.listenVelocity = listenVelocity;
    }

    public DraggableViewListener getDragListener() {
        return dragListener;
    }

    public void setDragListener(DraggableViewListener dragListener) {
        this.dragListener = dragListener;
    }

    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    public void setRotationEnabled(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }

    public float getRotationValue() {
        return rotationValue;
    }

    public void setRotationValue(float rotationValue) {
        this.rotationValue = rotationValue;
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setAnimating(boolean animating) {
        this.animating = animating;
    }

    public boolean isInlineMove() {
        return inlineMove;
    }

    public void setInlineMove(boolean inlineMove) {
        this.inlineMove = inlineMove;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    //translationX == 0 => 0 如果没有移动 getTranslationX = 0
    //-parentWidth/2 => -1 如果向左移动 getTranslationX < 0
    //parentWidth/2 => -1  如果向右移动 getTranslationX > 0
    public float getPercentX() {
        float percent = 2f * (ViewCompat.getTranslationX(this) - originalViewX) / getParentWidth();
        if (percent > 1) {
            percent = 1;
        }
        if (percent < -1) {
            percent = -1;
        }
        return percent;
    }

    //translationY == 0 => 0
    //-parentHeight/2 => -1
    //parentHeight/2 => -1
    public float getPercentY() {
        float percent = 2f * (ViewCompat.getTranslationY(this) - originalViewY) / getParentHeight();
        if (percent > 1) {
            percent = 1;
        }
        if (percent < -1) {
            percent = -1;
        }
        return percent;
    }

    public float getMaxDragPercentageY() {
        return maxDragPercentageY;
    }

    public void setMaxDragPercentageY(float maxDragPercentageY) {
        this.maxDragPercentageY = maxDragPercentageY;
    }

    public float getMaxDragPercentageX() {
        return maxDragPercentageX;
    }

    public void setMaxDragPercentageX(float maxDragPercentageX) {
        this.maxDragPercentageX = maxDragPercentageX;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);

        /**
        ACTION_DOWN 记录下第一次按下时的x,y坐标
        ACTION_MOVE 中判断是否左右或者上下滑动>10 则截断事件 让
         {@link #onTouchEvent(MotionEvent)}去处理截断的事件 并且返回true
         */
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                motionXOrigin = event.getRawX();
                motionYOrigin = event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                actionUp();
                break;
            case MotionEvent.ACTION_MOVE: {
                float newMotionX = event.getRawX();
                float newMotionY = event.getRawY();
                //left-right > 10 or top-bottom > 10 we need intercept touch event
                //why not use getScaledTouchSlop() instead ?
//                ViewConfiguration compat = ViewConfiguration.get()
//                compat.getScaledTouchSlop()
                return (Math.abs(motionXOrigin - newMotionX) > 10 || Math.abs(motionYOrigin - newMotionY) > 10);
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleTouch(event);
    }

    /**
     * 实时根据 getTranslationX 的大小来判断移动的方向及对应的percent比例
     */
    public void update() {
        float percentX = getPercentX();
        float percentY = getPercentY();
        update(percentX, percentY);
    }

    /**
     * 实时刷新拖拽的回调或更新比例
     * @param percentX
     * @param percentY
     */
    public void update(float percentX, float percentY) {
        if (rotationEnabled) {
            ViewCompat.setRotation(this, percentX * rotationValue);
        }

        if (dragListener != null) {
            dragListener.onDrag(this, percentX, percentY);
        }

        if (viewAnimator != null) {
            viewAnimator.update(this, percentX, percentY);
        }
        oldPercentX = percentX;
        oldPercentY = percentY;
    }

    public float getMinVelocity() {
        return minVelocity;
    }

    public void setMinVelocity(float minVelocity) {
        this.minVelocity = minVelocity;
    }

    public void reset() {
    }

    public float getOldPercentX() {
        return oldPercentX;
    }

    public float getOldPercentY() {
        return oldPercentY;
    }

    public float getOriginalViewX() {
        return originalViewX;
    }

    public void setOriginalViewX(float originalViewX) {
        this.originalViewX = originalViewX;
    }

    public float getOriginalViewY() {
        return originalViewY;
    }

    public void setOriginalViewY(float originalViewY) {
        this.originalViewY = originalViewY;
    }

    /**
     * 初始话DraggableView 移动的x,y距离,如果没执行过 translationX,Y返回0
     */
    public void initOriginalViewPositions() {
        this.originalViewX = ViewCompat.getTranslationX(this);
        this.originalViewY = ViewCompat.getTranslationY(this);
    }


    /**
     * 真正的滑动处理事件是在这里进行的
     * @param event
     * @return
     */
    boolean handleTouch(MotionEvent event) {
        if (draggable && !animating) {
            boolean handledByDetector = this.detector.onTouchEvent(event);
            if (!handledByDetector) {

                final int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    //ACTION_DOWN 在此根本是无用的 直接忽略掉
//                    case MotionEvent.ACTION_DOWN:
//                        //motionXOrigin = event.getRawX();
//                        //motionYOrigin = event.getRawY();
//                        break;
                    case MotionEvent.ACTION_UP:
                        //处理滑动结束ACTION_UP事件
                        actionUp();
                        break;
                    case MotionEvent.ACTION_MOVE: {
                        //通过滑动来获取当前最新的x,y坐标
                        float newMotionX = event.getRawX();
                        float newMotionY = event.getRawY();

                        float diffMotionX = newMotionX - motionXOrigin;
                        float diffMotionY = newMotionY - motionYOrigin;

                        //通过判断是否垂直滚动来让view执行垂直还是水平方向的位移
                        if (vertical) {
                            if (!inlineMove) {//级联移动?没懂
                                ViewCompat.setTranslationX(this, originalViewX + diffMotionX);
                            }
                            //在原始view的位置的 originalViewY + 滑动的距离
                            ViewCompat.setTranslationY(this, originalViewY + diffMotionY);
                        } else {
                            if (!inlineMove) {
                                ViewCompat.setTranslationY(this, originalViewY + diffMotionY);
                            }
                            ViewCompat.setTranslationX(this, originalViewX + diffMotionX);
                        }
                        //每次滑动得到最新的x,y坐标时都要实时计算更新现在滑动的比例
                        update();
                    }
                    break;
                }
            }

        }
        return true;
    }

    /**
     * Handle touch up action
     * How to dismiss draggable view.
     */
    void actionUp() {
        //get scroll direction
        float percentX = getPercentX();
        float percentY = getPercentY();

        if (viewAnimator != null) {
            boolean animated =
                (!vertical && percentX > maxDragPercentageX && animateExit(Direction.RIGHT)) ||
                    (!vertical && percentX < -maxDragPercentageX && animateExit(Direction.LEFT)) ||
                    (vertical && percentY > maxDragPercentageY && animateExit(Direction.BOTTOM)) ||
                    (vertical && percentY < -maxDragPercentageY && animateExit(Direction.TOP));
            //if user cannot custom animate exit method,we need animate view to origin location
            if (!animated) {
                animateToOrigin(ReturnOriginViewAnimator.ANIMATION_RETURN_TO_ORIGIN_DURATION);
            }
        }
    }

    float getParentWidth() {
        if (parentWidth == 0) {
            parentWidth = ((View) getParent()).getWidth();
        }
        return parentWidth;
    }

    float getParentHeight() {
        if (parentHeight == 0) {
            parentHeight = ((View) getParent()).getHeight();
        }
        return parentHeight;
    }

    /**
     * Animate Exit DraggableView
     * @param direction which direction
     * @return
     */
    boolean animateExit(Direction direction) {
        boolean animateExit = false;
        if (viewAnimator != null) {
            //let the interface handle animateExit
            animateExit = viewAnimator.animateExit(DraggableView.this, direction, DEFAULT_EXIT_DURATION);
        }

        if (animateExit) { //if need animate to exit. handle drag listener
            if (dragListener != null) {
                dragListener.onDraggedStarted(this, direction);
            }
        }

        return animateExit;
    }

    /**
     * DraggableView的初始化操作
     * @param context
     */
    private void initialize(Context context) {

        //手势监听--只监听 Fling 手势(滑动)
        //用GestureDetectorCompat来区别不同的版本变化
        detector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(@Nullable MotionEvent event1, @Nullable MotionEvent event2, float velocityX, float velocityY) {
                boolean animated = false;
                //need care about velocity,and user need call setMinVelocity first
                if (listenVelocity && !animating && viewAnimator != null && event1 != null && event2 != null) {
                    if (vertical) {//if vertical
                        if (Math.abs(velocityY) > minVelocity) {
                            //TODO 好像这里判断反了？
                            float distanceY = event1.getRawY() - event2.getRawY();
                            if (distanceY < 0) {
                                animated = animateExit(Direction.TOP);
                            } else {
                                animated = animateExit(Direction.BOTTOM);
                            }
                        }
                    } else {//horizontal
                        if (Math.abs(velocityX) > minVelocity) {
                            float distanceX = event1.getRawX() - event2.getRawX();
                            if (distanceX < 0) {
                                animated = animateExit(Direction.RIGHT);
                            } else {
                                animated = animateExit(Direction.LEFT);
                            }
                        }
                    }
                }
                return animated;
            }
        });

        //初始化viewAnimator 采用默认的实现 返回到原始的view位置
        this.viewAnimator = new ReturnOriginViewAnimator<DraggableView>() {
        };
    }

    public interface DraggableViewListener {
        void onDrag(DraggableView draggableView, float percentX, float percentY);

        void onDraggedStarted(DraggableView draggableView, Direction direction);

        void onDraggedEnded(DraggableView draggableView, Direction direction);

        void onDragCancelled(DraggableView draggableView);
    }

    public static abstract class DraggableViewListenerAdapter implements DraggableViewListener {
        @Override
        public void onDrag(DraggableView draggableView, float percentX, float percentY) {
        }

        @Override
        public void onDraggedStarted(DraggableView draggableView, Direction direction) {
        }

        @Override
        public void onDraggedEnded(DraggableView draggableView, Direction direction) {
        }

        @Override
        public void onDragCancelled(DraggableView draggableView) {
        }
    }

}
