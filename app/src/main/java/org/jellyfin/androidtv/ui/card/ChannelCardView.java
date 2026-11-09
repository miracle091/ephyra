package org.jellyfin.androidtv.ui.card;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import org.jellyfin.androidtv.R;
import org.jellyfin.androidtv.databinding.ViewCardChannelBinding;
import org.jellyfin.androidtv.ui.livetv.GuideStyle;
import org.jellyfin.androidtv.util.DateTimeExtensionsKt;
import org.jellyfin.sdk.model.api.BaseItemDto;

import java.time.LocalDateTime;

/**
 * Ephyra: a channel of the player's quick channel changer, in the look of the Live TV guide (GuideStyle): a surface-2 cell with
 * the ink ring on focus, mono times and the red dot before the programme on air.
 */
public class ChannelCardView extends FrameLayout {
    private static final String SEPARATOR = " · ";

    private ViewCardChannelBinding binding = ViewCardChannelBinding.inflate(LayoutInflater.from(getContext()), this, true);

    public ChannelCardView(Context context) {
        super(context);
        setBackground(GuideStyle.cellBackground(context));
        // Ephyra: the progress bar hugs the bottom edge, so the cell's rounded corners clip it
        setClipToOutline(true);
        binding.time.setTypeface(GuideStyle.monoTypeface(context));
    }

    public void setItem(final BaseItemDto channel) {
        if (channel.getNumber() != null) binding.name.setText(channel.getNumber() + SEPARATOR + channel.getName());
        else binding.name.setText(channel.getName());

        BaseItemDto program = channel.getCurrentProgram();
        if (program != null) {
            updateDisplay(program);
        } else {
            showNoProgram();
        }
    }

    private void showNoProgram() {
        binding.program.setText(R.string.no_program_data);
        binding.program.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        binding.time.setText("");
        binding.progress.setVisibility(View.INVISIBLE);
    }

    private void updateDisplay(BaseItemDto program) {
        binding.program.setText(program.getName());
        binding.program.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ephyra_guide_live_dot, 0, 0, 0);

        if (program.getStartDate() != null && program.getEndDate() != null) {
            binding.time.setText(new StringBuilder()
                    .append(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getStartDate()))
                    .append(" - ")
                    .append(DateTimeExtensionsKt.getTimeFormatter(getContext()).format(program.getEndDate()))
            );

            int now = GuideStyle.nowProgress(program.getStartDate(), program.getEndDate(), LocalDateTime.now());
            if (now >= 0) {
                binding.progress.setProgress(now);
                binding.progress.setVisibility(View.VISIBLE);
            } else {
                // not on air: no dot, no bar
                binding.program.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                binding.progress.setVisibility(View.INVISIBLE);
            }
        } else {
            binding.time.setText("");
            binding.progress.setVisibility(View.INVISIBLE);
        }
    }
}
