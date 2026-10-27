package com.frantv.premium;

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

/** Lista de canales del reproductor (panel lateral). */
public class PanelAdapter extends RecyclerView.Adapter<PanelAdapter.VH> {

    public interface Listener {
        void onClick(int pos);
    }

    private List<Channel> data = new ArrayList<>();
    private String currentId = "";
    private final Listener listener;

    public PanelAdapter(Listener l) {
        this.listener = l;
    }

    public void setData(List<Channel> list) {
        data = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    public void setCurrent(String id) {
        currentId = id == null ? "" : id;
        notifyDataSetChanged();
    }

    public int indexOf(String id) {
        for (int i = 0; i < data.size(); i++) if (data.get(i).id.equals(id)) return i;
        return -1;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_panel_channel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Channel c = data.get(position);
        boolean cur = c.id.equals(currentId);
        h.num.setText(String.format(Locale.US, "%03d", position + 1));
        h.name.setText(c.name);
        h.eq.setVisibility(cur ? View.VISIBLE : View.GONE);
        h.itemView.setSelected(cur);
        h.star.setImageResource(Repo.isFav(c.id) ? R.drawable.ic_star_gold : R.drawable.ic_star_outline);
        h.star.setAlpha(Repo.isFav(c.id) ? 1f : 0.55f);
        if (c.logo == null || c.logo.isEmpty()) {
            Glide.with(h.logo).clear(h.logo);
            h.logo.setImageDrawable(new InitialsDrawable(c));
        } else {
            Glide.with(h.logo).load(c.logo).error(new InitialsDrawable(c)).fitCenter().into(h.logo);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onViewRecycled(@NonNull VH h) {
        Glide.with(h.logo).clear(h.logo);
    }

    class VH extends RecyclerView.ViewHolder {
        final ImageView logo, star, eq;
        final TextView num, name;

        VH(@NonNull View v) {
            super(v);
            logo = v.findViewById(R.id.logo);
            star = v.findViewById(R.id.star);
            eq = v.findViewById(R.id.eq);
            num = v.findViewById(R.id.num);
            name = v.findViewById(R.id.name);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int p = getBindingAdapterPosition();
                    if (p != RecyclerView.NO_POSITION) listener.onClick(p);
                }
            });
        }
    }
}
