package comp5216.sydney.edu.au.unichat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> userMessageList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    Bitmap myBitmap;

    public MessageAdapter(List<Messages> userMessageList){
        this.userMessageList = userMessageList;
    }


    public class MessageViewHolder extends RecyclerView.ViewHolder{
        public TextView senderMessageText, receiverMessageText;
        public ImageView senderImage, receiverImage;

        public MessageViewHolder(View itemView) {
            super(itemView);

            senderMessageText = (TextView) itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = (TextView) itemView.findViewById(R.id.receiver_message_text);
            senderImage=(ImageView)itemView.findViewById(R.id.sender_message_image);
            receiverImage =(ImageView)itemView.findViewById(R.id.receiver_message_image);
        }
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_messages_layout,parent,false);

        mAuth = FirebaseAuth.getInstance();

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, int position) {
        String messageSenderId = mAuth.getCurrentUser().getUid();
        final Messages messages = userMessageList.get(position);

        String fromUserID =  messages.getFrom();
        String fromMessageType = messages.getType();

        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);

        if(fromMessageType.equals("text")){
            holder.receiverMessageText.setVisibility(View.INVISIBLE);
            holder.senderMessageText.setVisibility(View.INVISIBLE);
            holder.senderImage.setVisibility(View.INVISIBLE);
            holder.receiverImage.setVisibility(View.INVISIBLE);
            holder.senderImage.getLayoutParams().height = 0;
            holder.receiverImage.getLayoutParams().height = 0;

            if(fromUserID.equals(messageSenderId)){
                holder.senderMessageText.setVisibility(View.VISIBLE);
                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setTextColor(Color.BLACK);
                holder.senderMessageText.setText(messages.getMessage());
            }else{
                holder.receiverMessageText.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setTextColor(Color.BLACK);
                holder.receiverMessageText.setText(messages.getMessage());
            }
        } else{
            holder.receiverMessageText.setVisibility(View.INVISIBLE);
            holder.senderMessageText.setVisibility(View.INVISIBLE);
            holder.senderImage.setVisibility(View.INVISIBLE);
            holder.receiverImage.setVisibility(View.INVISIBLE);
            holder.senderImage.getLayoutParams().height = WRAP_CONTENT;
            holder.receiverImage.getLayoutParams().height = WRAP_CONTENT;
            holder.senderImage.setMaxHeight(750);
            holder.receiverImage.setMaxHeight(750);

            final String imageID=messages.getImageID();

            if(fromUserID.equals(messageSenderId)){
                holder.senderImage.setVisibility(View.VISIBLE);

                File imgFile = new File(android.os.Environment.getExternalStorageDirectory().getPath()+"/Unichat/images/"+imageID+".jpg");
                if(imgFile.exists()){
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    holder.senderImage.setImageBitmap(myBitmap);
                }else{
                    Picasso.get().load(messages.getImage()).into(holder.senderImage, new com.squareup.picasso.Callback(){

                        @Override
                        public void onSuccess() {
                            Picasso.get()
                                    .load(messages.getImage())
                                    .into(picassoImageTarget(imageID));
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
                    Picasso.get().load(messages.getImage()).into(holder.senderImage);
                }


            }else{
                holder.receiverImage.setVisibility(View.VISIBLE);

                File imgFile = new  File(android.os.Environment.getExternalStorageDirectory().getPath()+"/Unichat/images/"+imageID+".jpg");
                if(imgFile.exists()){
                    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    holder.receiverImage.setImageBitmap(myBitmap);
                }else{
                    Picasso.get().load(messages.getImage()).into(holder.receiverImage, new com.squareup.picasso.Callback(){

                        @Override
                        public void onSuccess() {
                            Picasso.get()
                                    .load(messages.getImage())
                                    .into(picassoImageTarget(imageID));
                        }

                        @Override
                        public void onError(Exception e) {

                        }
                    });
                    Picasso.get().load(messages.getImage()).into(holder.receiverImage);
                }


            }
        }


    }

    @Override
    public int getItemCount() {
        return userMessageList.size();
    }

    private Target picassoImageTarget(final String imageName) {
        final File directory = new File(android.os.Environment.getExternalStorageDirectory().getPath()+"/Unichat/images/");
        return new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File myImageFile = new File(directory, imageName+".jpg"); // Create image file
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(myImageFile);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("image", "image saved to >>>" + myImageFile.getAbsolutePath());
                    }
                }).start();

            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {


            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                if (placeHolderDrawable != null) {}
            }
        };
    }

}