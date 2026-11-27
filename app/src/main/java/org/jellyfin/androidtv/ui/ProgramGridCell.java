package org.jellyfin.androidtv.ui;

import static org.koin.java.KoinJavaComponent.get;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.preference.LiveTvPreferences;
import org.jellyfin.androidtv.ui.livetv.GuideStyle;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuide;
import org.jellyfin.androidtv.ui.livetv.LiveTvGuideFragmentHelperKt;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.time.LocalDateTime;

public class ProgramGridCell extends RelativeLayout implements RecordingIndicatorView {
    // Velella: how much a programme already over is faded
    private static final float PAST_ALPHA = 0.45f;

    private LiveTvGuide mActivity;
    private TextView mProgramName;
    private LinearLayout mInfoRow;
    private BaseItemDto mProgram;
    private ImageView mRecIndicator;
    private int mBackgroundColor = 0;
    // Velella: the on-air state the cell is drawn in, and the views refreshNow changes with it
    private boolean mLive;
    private boolean mOver;
    private View mContent;
    private TextView mStartTime;
    private TextView mLiveLabel;
    private TextView mTimeLeft;
    private boolean isLast;
    private boolean isFirst;

    public ProgramGridCell(Context context, LiveTvGuide activity, BaseItemDto program, boolean keyListen) {
        super(context);
        initComponent((Activity) context, activity, program, keyListen);
    }

    private void initComponent(Activity context, LiveTvGuide activity, BaseItemDto program, boolean keyListen) {
        mActivity = activity;

        LayoutInflater inflater = LayoutInflater.from(context);
        mContent = inflater.inflate(R.layout.program_grid_cell, this, false);
        this.addView(mContent);

        setFocusable(true);
        // Velella: the elapsed fill reaches the cell's edges, so the cell's rounded corners clip it
        setClipToOutline(true);

        mProgramName = findViewById(R.id.programName);
        mInfoRow = findViewById(R.id.infoRow);
        mLiveLabel = findViewById(R.id.liveLabel);
        mTimeLeft = findViewById(R.id.timeLeft);
        mProgramName.setText(program.getName());
        mProgram = program;
        mRecIndicator = findViewById(R.id.recIndicator);

        setCellBackground();

        if (program.getStartDate() != null && program.getEndDate() != null) {
            LocalDateTime localStart = program.getStartDate();
            if (localStart.plusMinutes(1).isBefore(activity.getCurrentLocalStartDate())) {
                // Velella: no "<<" before the name; the start time under it tells the programme began earlier
                mStartTime = new TextView(context);
                mStartTime.setTypeface(GuideStyle.monoTypeface(context));
                mStartTime.setTextColor(Utils.getThemeColor(context, R.attr.controlIconForegroundInactive));
                mStartTime.setTextSize(12);
                mStartTime.setText(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getStartDate()));
                mInfoRow.addView(mStartTime);
            }
        }

        LiveTvPreferences liveTvPreferences = get(LiveTvPreferences.class);

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowNewIndicator()) && BaseItemExtensionsKt.isNew(program) && (!liveTvPreferences.get(LiveTvPreferences.Companion.getShowPremiereIndicator()) || !Utils.isTrue(program.isPremiere()))) {
            addBlockText(context.getString(R.string.lbl_new));
        }

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowPremiereIndicator()) && Utils.isTrue(program.isPremiere())) {
            addBlockText(context.getString(R.string.lbl_premiere));
        }

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowRepeatIndicator()) && Utils.isTrue(program.isRepeat())) {
            addBlockText(context.getString(R.string.lbl_repeat));
        }

        if (program.getOfficialRating() != null && !program.getOfficialRating().equals("0")) {
            addBlockText(program.getOfficialRating());
        }

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getShowHDIndicator()) && Utils.isTrue(program.isHd())) {
            addBlockText("HD");
        }

        if (program.getSeriesTimerId() != null) {
            mRecIndicator.setImageResource(program.getTimerId() != null ? R.drawable.ic_record_series_red : R.drawable.ic_record_series);
        } else if (program.getTimerId() != null) {
            mRecIndicator.setImageResource(R.drawable.ic_record_red);
        }


        refreshNow();

        if (keyListen) {
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mActivity.showProgramOptions();
                }
            });
        }

    }

    /**
     * Velella: the programme on air gets a lighter cell, its elapsed part filled in the theme's accent up to the current time, and
     * an "on air" tag with the time left in place of its start time; programmes already over are faded. Called again by the
     * guide's ticker, so the marks move on to the next programme without reloading.
     */
    public void refreshNow() {
        ProgressBar nowBar = findViewById(R.id.nowBar);
        int now = -1;
        boolean over = false;
        LocalDateTime time = LocalDateTime.now();
        if (mProgram != null && !LiveTvGuideFragmentHelperKt.isNoProgramData(mProgram) && mProgram.getStartDate() != null
                && mProgram.getEndDate() != null) {
            now = GuideStyle.nowProgress(mProgram.getStartDate(), mProgram.getEndDate(), time);
            over = !time.isBefore(mProgram.getEndDate());
        }
        boolean live = now >= 0;
        if (live) {
            // The cell starts at the guide's start when the programme began earlier: the fill runs over that visible part
            LocalDateTime visibleStart = GuideStyle.visibleStart(mProgram.getStartDate(), mActivity.getCurrentLocalStartDate());
            nowBar.setProgress(Math.max(0, GuideStyle.nowProgress(visibleStart, mProgram.getEndDate(), time)));
            mTimeLeft.setText(GuideStyle.timeLeftText(getContext(), mProgram.getEndDate(), time));
        }
        int liveVisibility = live ? View.VISIBLE : View.GONE;
        nowBar.setVisibility(liveVisibility);
        mLiveLabel.setVisibility(liveVisibility);
        mTimeLeft.setVisibility(liveVisibility);
        if (mStartTime != null) mStartTime.setVisibility(live ? View.GONE : View.VISIBLE);
        mOver = over;
        applyFade();
        if (live != mLive) {
            mLive = live;
            applyBackground();
        }
    }

    private void addBlockText(String text) {
        TextView view = new TextView(getContext());
        view.setTextSize(10);
        view.setTextColor(Utils.getThemeColor(getContext(), R.attr.controlIconForegroundInactive));
        view.setText(text);
        view.setBackgroundResource(R.drawable.velella_guide_badge);
        int sidePadding = Utils.convertDpToPixel(getContext(), 4);
        view.setPadding(sidePadding, 0, sidePadding, 0);
        mInfoRow.addView(view);
    }

    public void setCellBackground() {
        LiveTvPreferences liveTvPreferences = get(LiveTvPreferences.class);

        if (liveTvPreferences.get(LiveTvPreferences.Companion.getColorCodeGuide())) {
            if (Utils.isTrue(mProgram.isMovie())) {
                mBackgroundColor = getResources().getColor(R.color.guide_movie_bg);
            } else if (Utils.isTrue(mProgram.isNews())) {
                mBackgroundColor = getResources().getColor(R.color.guide_news_bg);
            } else if (Utils.isTrue(mProgram.isSports())) {
                mBackgroundColor = getResources().getColor(R.color.guide_sports_bg);
            } else if (Utils.isTrue(mProgram.isKids())) {
                mBackgroundColor = getResources().getColor(R.color.guide_kids_bg);
            }
        }
        applyBackground();
    }

    // Velella: a programme already over is faded, but not while it has focus, so it stays readable
    private void applyFade() {
        mContent.setAlpha(mOver && !hasFocus() ? PAST_ALPHA : 1f);
    }

    private void applyBackground() {
        setBackground(GuideStyle.cellBackground(getContext(), mBackgroundColor, mLive));
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        // Velella: a name too long for its cell scrolls while the cell has focus (the marquee runs only on a selected view)
        mProgramName.setSelected(gainFocus);
        applyFade();

        if (gainFocus) {
            mActivity.setSelectedProgram(this);
        }
    }

    public BaseItemDto getProgram() { return mProgram; }

    public void setLast() { isLast = true; }
    public boolean isLast() { return isLast; }
    public void setFirst() { isFirst = true; }
    public boolean isFirst() { return isFirst; }

    public void setRecTimer(String id) {
        if (mProgram == null) return;
        mProgram = LiveProgramDetailPopupHelperKt.copyWithTimerId(mProgram, id);
        mRecIndicator.setImageResource(id != null ? (mProgram.getSeriesTimerId() != null ? R.drawable.ic_record_series_red : R.drawable.ic_record_red) : mProgram.getSeriesTimerId() != null ? R.drawable.ic_record_series : R.drawable.blank10x10);
    }
    public void setRecSeriesTimer(String id) {
        if (mProgram == null) return;
        mProgram = LiveProgramDetailPopupHelperKt.copyWithSeriesTimerId(mProgram, id);
        mRecIndicator.setImageResource(id != null ? R.drawable.ic_record_series_red : R.drawable.blank10x10);
    }
}
