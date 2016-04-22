package de.piobyte.barnearby.adapter;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.client.Firebase;
import com.firebase.ui.FirebaseRecyclerAdapter;
import com.github.florent37.glidepalette.GlidePalette;

import de.piobyte.barnearby.R;
import de.piobyte.barnearby.data.Locality;

public class LocalitiesAdapter extends FirebaseRecyclerAdapter<Locality,
        LocalitiesAdapter.ViewHolder> {

    private static OnItemClickListener sListener;

    public interface OnItemClickListener {
        void onItemClick(int position, View sharedElement);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final View textBackgroundView;
        private final TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
            textBackgroundView = itemView.findViewById(R.id.text_background);
            textView = (TextView) itemView.findViewById(R.id.text);

//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    sListener.onItemClick(getAdapterPosition(), imageView);
//                }
//            });
        }
    }

    public LocalitiesAdapter(Firebase firebase, OnItemClickListener listener) {
        super(Locality.class, R.layout.grid_item, ViewHolder.class, firebase);
        sListener = listener;
    }

    @Override
    protected void populateViewHolder(ViewHolder holder, Locality locality, int position) {
        View sharedElement = holder.imageView;
        String transitionName = locality.getImage();
        ViewCompat.setTransitionName(sharedElement, transitionName);
        populateImage(holder, locality.getImage());
        holder.textView.setText(locality.getName());
    }

    private void populateImage(final ViewHolder holder, String image) {
        Glide.with(holder.itemView.getContext())
                .load(image)
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .centerCrop()
                .listener(GlidePalette.with(image)
                        .use(GlidePalette.Profile.VIBRANT)
                        .intoBackground(holder.textBackgroundView))
                .into(holder.imageView);
    }
}