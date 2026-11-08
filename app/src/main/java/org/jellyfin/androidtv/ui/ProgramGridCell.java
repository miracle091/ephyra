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
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.androidtv.util.Utils;
import org.jellyfin.androidtv.util.sdk.BaseItemExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.time.LocalDateTime;

public class ProgramGridCell extends RelativeLayout implements RecordingIndicatorView {

    private LiveTvGuide mActivity;
    private TextView mProgramName;
    private LinearLayout mInfoRow;
    private BaseItemDto mProgram;
    private ImageView mRecIndicator;
    private int mBackgroundColor = 0;
    private boolean isLast;
    private boolean isFirst;

    public ProgramGridCell(Context context, LiveTvGuide activity, BaseItemDto program, boolean keyListen) {
        super(context);
        initComponent((Activity) context, activity, program, keyListen);
    }

    private void initComponent(Activity context, LiveTvGuide activity, BaseItemDto program, boolean keyListen) {
        mActivity = activity;

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.program_grid_cell, this, false);
        this.addView(v);

        setFocusable(true);
        setBackground(GuideStyle.cellBackground(context));

        mProgramName = findViewById(R.id.programName);
        mInfoRow = findViewById(R.id.infoRow);
        mProgramName.setText(program.getName());
        mProgram = program;
        mRecIndicator = findViewById(R.id.recIndicator);

        setCellBackground();

        if (program.getStartDate() != null && program.getEndDate() != null) {
            LocalDateTime localStart = program.getStartDate();
            if (localStart.plusMinutes(1).isBefore(activity.getCurrentLocalStartDate())) {
                // Ephyra: no "<<" before the name; the start time under it tells the programme began earlier
                TextView time = new TextView(context);
                time.setTypeface(GuideStyle.monoTypeface(context));
                time.setTextColor(Utils.getThemeColor(context, R.attr.controlIconForegroundInactive));
                time.setTextSize(12);
                time.setText(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getStartDate()));
                mInfoRow.addView(time);
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
     * Ephyra: the programme on air gets a red dot before its name, like the player's LIVE badge, and a bar in the theme's accent
     * that runs with the clock. Called again by the guide's ticker, so both move on to the next programme without reloading.
     */
    public void refreshNow() {
        ProgressBar nowBar = findViewById(R.id.nowBar);
        int now = -1;
        if (mProgram != null && mProgram.getStartDate() != null && mProgram.getEndDate() != null) {
            now = GuideStyle.nowProgress(mProgram.getStartDate(), mProgram.getEndDate(), LocalDateTime.now());
        }
        if (now >= 0) {
            nowBar.setProgress(now);
            nowBar.setVisibility(View.VISIBLE);
            mProgramName.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ephyra_guide_live_dot, 0, 0, 0);
        } else {
            nowBar.setVisibility(View.GONE);
            mProgramName.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    private void addBlockText(String text) {
        TextView view = new TextView(getContext());
        view.setTextSize(10);
        view.setTextColor(Utils.getThemeColor(getContext(), R.attr.controlIconForegroundInactive));
        view.setText(text);
        view.setBackgroundResource(R.drawable.ephyra_guide_badge);
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

            setBackground(GuideStyle.cellBackground(getContext(), mBackgroundColor));
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

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
