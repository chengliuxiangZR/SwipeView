package com.example.asus.swipecards;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

public class SwipeCards extends ViewGroup {
    private static final String TAG="SwipeCards";

    private int mCenterX;
    private int mCenterY;

    private ViewDragHelper mViewDragHelper;

    private static final int MAX_DEGREE=60;
    private static final float MAX_ALPHA_RANGE=0.5f;

    private int mCardGap=(int ) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8,getResources().getDisplayMetrics());

    public SwipeCards(Context context) {
        this(context,null);
    }

    public SwipeCards(Context context, AttributeSet attrs) {
        super(context, attrs);
        //触摸事件处理第一步，初始化ViewDragHelper
        mViewDragHelper=ViewDragHelper.create(this,mCallback);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //测量所有的CardView
        measureChildren(widthMeasureSpec,heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //获取控件中心位置,为布局CardView做准备，
        // 将CardView都布局在SwipeCards的中心位置，
        // 只是每个CardView在竖直方法都有一个间隔,
        // 先布局的CardView的位置会偏下
        mCenterX=w/2;
        mCenterY=h/2;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //布局所有的CardView
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);
            int left=mCenterX-child.getMeasuredWidth()/2;
            //mCardGap为卡片之间在竖直方向的间隔，先布局的CardView的位置会偏下
            int top=mCenterY-child.getMeasuredHeight()/2+mCardGap*(getChildCount()-i);
            int right=left+child.getMeasuredWidth();
            int bottom=top+child.getMeasuredHeight();
            child.layout(left,top,right,bottom);

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        触摸事件处理第二步,ViewDragHelper对象处理触摸事件
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    //触摸事件处理第三步,处理ViewDragHelper对象处理触摸事件的回调
    private ViewDragHelper.Callback mCallback=new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            //返回true，表示允许拖动所有的“孩子”
            return true;
        }

        @Override
        public int clampViewPositionHorizontal(@NonNull View child, int left, int dx) {
            //使用ViewDragHelper提供的child的left位置
            return left;
        }

        @Override
        public int clampViewPositionVertical(@NonNull View child, int top, int dy) {
            //使用ViewDragHelper提供的child的top位置
            return top;
        }

        //让CardView在拖动时产生一定的旋转和透明度的变化
        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            //计算位置改变后，与原来位置的中心变化量
            int diffX=left+changedView.getWidth()/2-mCenterX;
            //计算变化量占SwipeCards宽度的比值
            float ratio=diffX*1.0f/getWidth();
            //计算在left位置时，changedView应该具有的旋转角度，MAX_DEGREE为角度的最大变化量
            float degree=MAX_DEGREE*ratio;
            //设置旋转
            changedView.setRotation(degree);
            //计算left位置时，changedView应该具有的透明度，MAX_ALPHA_RANGE为透明度最大变化量
            float alpha=1-Math.abs(ratio)*MAX_ALPHA_RANGE;
            //设置透明度
            changedView.setAlpha(alpha);
        }

        //当用户松开时的情况
        @Override
        public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
            final int left=releasedChild.getLeft();
            final int right=releasedChild.getRight();
            if(left>mCenterX){
                //如果松开时releasedChild的left位置大于中心点X轴坐标，则往右边飞出
                animateToRight(releasedChild);
            }else if(right<mCenterX){
                //如果松开时releasedChild的right位置小于中心点X轴坐标，则往左边飞出
                animateToLeft(releasedChild);
            }else {
                //如果托动幅度不大，回到中间位置
                animateToCenter(releasedChild);
            }
        }
    };
    private void animateToRight(View releasedChild){
        //计算滚动结束后的左边位置，能够让releasedChild不见即可
        int finalLeft=getWidth()+releasedChild.getHeight();
        //计算动画结束后的顶部位置
        int finalTop=releasedChild.getTop();
        //平滑滚动releasedView,smoothSlideViewTo内部会调用到Scroller的startScroll方法
        mViewDragHelper.smoothSlideViewTo(releasedChild,finalLeft,finalTop);
        //触发重新绘制
        invalidate();
    }

    private void animateToCenter(View releasedChild) {
        int finalLeft = mCenterX - releasedChild.getWidth() / 2;
        int indexOfChild = indexOfChild(releasedChild);
        int finalTop = mCenterY - releasedChild.getHeight() / 2 + mCardGap * (getChildCount() - indexOfChild);
        mViewDragHelper.smoothSlideViewTo(releasedChild, finalLeft, finalTop);
        invalidate();
    }

    private void animateToLeft(View releasedChild) {
        int finalLeft = -getWidth();
        int finalTop = 0;
        mViewDragHelper.smoothSlideViewTo(releasedChild, finalLeft, finalTop);
        invalidate();
    }

    @Override
    public void computeScroll() {
        //continueSettling内部调用了Scroller的computeScrollOffset计算滚动偏移量，
        //并且完成了滚动
        //当动画结束continueSettling返回false，没有结束返回true
        if(mViewDragHelper.continueSettling(false)){
            invalidate();
        }
    }
}
