package com.frantv.premium;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.VH> {

    public interface Listener {
        void onClick(Channel c, int pos);

        void onLongClick(Channel c, int pos);

        boolean onKey(int keyCode, int pos);

        void onFocus(int pos);
    }

    private List<Channel> data = new ArrayList<>();
    private final Listener listener;

    public ChannelAdapter(Listener l) {
        this.listener = l;
    }

    public void setData(List<Channel> list) {
        data = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public List<Channel> getData() {
        return data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_channel, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Channel c = data.get(position);
        h.num.setText(String.format(Locale.US, "%03d", position + 1));
        h.name.setText(c.name);
        h.star.setVisibility(Repo.isFav(c.id) ? View.VISIBLE : View.GONE);
        Util.loadLogo(h.logo, c);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onViewRecycled(@NonNull VH h) {
        Glide.with(h.logo).clear(h.logo);
        h.logo.setBackground(null);
    }

    class VH extends RecyclerView.ViewHolder {
        final ImageView logo, star;
        final TextView num, name;

        VH(@NonNull View v) {
            super(v);
            logo = v.findViewById(R.id.logo);
            star = v.findViewById(R.id.star);
            num = v.findViewById(R.id.num);
            name = v.findViewById(R.id.name);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int p = getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) listener.onClick(data.get(p), p);
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int p = getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) listener.onLongClick(data.get(p), p);
                    return true;
                }
            });
            v.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent event) {
                    if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
                    int p = getBindingAdapterPosition();
                    if (p == RecyclerView.NO_POSITION) return false;
                    return listener.onKey(keyCode, p);
                }
            });
            v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    view.animate().scaleX(hasFocus ? 1.07f : 1f).scaleY(hasFocus ? 1.07f : 1f)
                            .setDuration(130).start();
                    view.setTranslationZ(hasFocus ? 12f : 0f);
                    if (hasFocus) {
                        int p = getBindingAdapterPosition();
                        if (p != RecyclerView.NO_POSITION) listener.onFocus(p);
                    }
                }
            });
        }
    }
}
