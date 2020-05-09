package org.ar.ar_android_tutorial_1to1;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import org.webrtc.PercentFrameLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ARVideoGroup {

    //所有视频View的容器
    public RelativeLayout m_rl_video_group;
    private LinkedHashMap<String, VideoView> m_list_video;
    private int Hspace = 2;
    private int Vspace = 2;
    private int VIDEO_WIDTH;
    private int VIDEO_HEIGHT;
    private int SCREEN_WIDTH;//屏幕宽
    private int SCREEN_HEIGHT;//屏幕高
    private int VIDEO_NUM = 3;//一排要显示多少个像  会影响小像显示的大小

    public ARVideoGroup(Activity activity, RelativeLayout m_rl_video_group) {
        this.m_rl_video_group = m_rl_video_group;
        m_list_video = new LinkedHashMap<>();
        initVideoSize(activity);
    }

    private void initVideoSize(Activity activity) {
        Point size = new Point();
        Display display = activity.getWindowManager().getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= 17)
            display.getRealSize(size);
        else
            display.getSize(size);
        SCREEN_WIDTH = size.x;
        SCREEN_HEIGHT = size.y;
        //4:3比例
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {//横屏
            VIDEO_WIDTH = (int) (((SCREEN_WIDTH / VIDEO_NUM)) / (SCREEN_WIDTH / 100f));
            VIDEO_HEIGHT = (int) (((SCREEN_WIDTH / VIDEO_NUM)) / 1.333333f) / (SCREEN_HEIGHT / 100);
        } else {
            VIDEO_WIDTH = (int) (((SCREEN_WIDTH / VIDEO_NUM)) / (SCREEN_WIDTH / 100f));
            VIDEO_HEIGHT = (int) (((SCREEN_WIDTH / VIDEO_NUM)) * 1.333333f) / (SCREEN_HEIGHT / 100);
        }
    }


    //一个VideoView 就是一个装了视频View对象
    protected class VideoView {
        public String videoId;
        public TextureView videoView;
        public PercentFrameLayout mLayout;
        private Context context;
        private FrameLayout fl_video;
        public int index;

        public VideoView(String videoId, TextureView videoView) {
            this.videoId = videoId;
            this.videoView = videoView;
            this.context = videoView.getContext();
            mLayout = new PercentFrameLayout(context);
            mLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            View view = View.inflate(context, R.layout.layout_video, null);//这个View可完全自定义 需要显示名字或者其他图标可以在里面加
            fl_video = view.findViewById(R.id.fl_video);
            fl_video.addView(videoView);
            mLayout.addView(view);
        }
    }


    public void addView(String vieoId, TextureView video) {
        VideoView videoView = new VideoView(vieoId, video);
        m_list_video.put(vieoId, videoView);
        m_rl_video_group.addView(videoView.mLayout);
        updateLayout();
    }

    public void removeView(String vieoId) {
        if (m_list_video.containsKey(vieoId)) {
            m_rl_video_group.removeView(m_list_video.get(vieoId).mLayout);
            m_rl_video_group.requestLayout();
        }
        m_list_video.remove(vieoId);
        updateLayout();
    }

    public void removeAllView() {
        m_rl_video_group.removeAllViews();
        m_list_video.clear();
    }


    private void updateLayout() {
        //改变位置 大小 自己修改
        List<Map.Entry<String, VideoView>> list = new ArrayList<Map.Entry<String, VideoView>>(m_list_video.entrySet());
        for (int i = 0; i < list.size(); i++) {//排序  index=0的 即为大屏
            list.get(i).getValue().index = i;
        }
        if (m_list_video.size() <= 2) {//1个本地像和1个远程像
            Iterator<Map.Entry<String, VideoView>> iter = m_list_video.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoView> entry = iter.next();
                VideoView render = entry.getValue();
                if (render.index == 0) {
                    render.mLayout.setPosition(0, 0, 100, 100);
                } else {
                    render.mLayout.setPosition(100-VIDEO_WIDTH-Hspace, Vspace, VIDEO_WIDTH, VIDEO_HEIGHT);
                }
                render.mLayout.requestLayout();
                m_rl_video_group.requestLayout();
            }
        } else {//3个及以上
            Iterator<Map.Entry<String, VideoView>> iter = m_list_video.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoView> entry = iter.next();
                VideoView render = entry.getValue();
                switch (render.index) {
                    case 0:
                        render.mLayout.setPosition(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 1:
                        render.mLayout.setPosition(VIDEO_WIDTH, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 2:
                        render.mLayout.setPosition(VIDEO_WIDTH*2, 0, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 3:
                        render.mLayout.setPosition(0, VIDEO_HEIGHT, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 4:
                        render.mLayout.setPosition(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 5:
                        render.mLayout.setPosition(VIDEO_WIDTH*2, VIDEO_HEIGHT, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 6:
                        render.mLayout.setPosition(0, VIDEO_HEIGHT*2, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 7:
                        render.mLayout.setPosition(VIDEO_WIDTH, VIDEO_HEIGHT*2, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                    case 8:
                        render.mLayout.setPosition(VIDEO_WIDTH*2, VIDEO_HEIGHT*2, VIDEO_WIDTH, VIDEO_HEIGHT);
                        break;
                }
                render.mLayout.requestLayout();
                m_rl_video_group.requestLayout();
            }
        }
    }
}
