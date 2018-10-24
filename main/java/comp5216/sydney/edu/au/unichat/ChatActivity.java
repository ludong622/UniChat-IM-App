package comp5216.sydney.edu.au.unichat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID, messageReceiverName, messageReceiverImage, messageSenderID, messageReceiverImageID, currentDate, currentTime;

    private TextView userName, userLastSeen;
    private CircleImageView userImage;

    private Toolbar ChatToolBar;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef, messageSender, messageReceiver;
    private StorageReference userChatImgRef,filePath;

    private ImageButton SendMessageButton;
    private EditText MessageInputText;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private RecyclerView userMessagesList;

    private ImageButton sendImageBtn;
    private ProgressDialog loadingBar;
    private File compressedImage;
    private String currentID;
    private ChildEventListener eventListener;
    Bitmap newMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        currentID=mAuth.getUid();
        messageSenderID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        userChatImgRef = FirebaseStorage.getInstance().getReference().child("Chat Images").child(currentID);

        messageReceiverID = getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("visit_user_name").toString();
        messageReceiverImage = getIntent().getExtras().get("visit_image").toString();
        messageReceiverImageID = getIntent().getExtras().get("visit_image_id").toString();

        InitializeControllers();

        userName.setText(messageReceiverName);
        File imgFile = new File(android.os.Environment.getExternalStorageDirectory().getPath()+"/Unichat/images/"+messageReceiverImageID+".jpg");
        if(!imgFile.exists()){
            Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);
        }else {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            userImage.setImageBitmap(myBitmap);
        }


        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();
            }
        });

        sendImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(ChatActivity.this);
            }
        });
    }

    private void InitializeControllers() {

        ChatToolBar=(Toolbar)findViewById(R.id.chat_toolbar);

        setSupportActionBar(ChatToolBar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        userImage = (CircleImageView) findViewById(R.id.custom_profile_image);
        userName = (TextView) findViewById(R.id.custom_profile_name);

        SendMessageButton=(ImageButton)findViewById(R.id.send_message_btn);
        MessageInputText = (EditText)findViewById(R.id.input_message);
        sendImageBtn = findViewById(R.id.send_picture_btn);

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = (RecyclerView)findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        loadingBar= new ProgressDialog(this);

         eventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);

                messagesList.add(messages);

                messageAdapter.notifyDataSetChanged();

                userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };


    }

    @Override
    protected void onPause() {
        super.onPause();
        messagesList.clear();
        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).removeEventListener(eventListener);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //messageAdapter = new MessageAdapter(messagesList);


        RootRef.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(eventListener);
    }

    private void SendMessage(){

        Date date = new Date();

        String messageText = MessageInputText.getText().toString();

        if(TextUtils.isEmpty(messageText)){
            Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        }else{
            String messageSenderRef = "Messages/" + messageSenderID + "/" +messageReceiverID;
            String messageReceiverRef = "Messages/" + messageReceiverID + "/" +messageSenderID;

            DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            messageSender = RootRef.child("Contacts").child(messageSenderID).child(messageReceiverID);
            messageReceiver = RootRef.child("Contacts").child(messageReceiverID).child(messageSenderID);

            String messagePushID = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message",messageText);
            messageTextBody.put("type","text");
            messageTextBody.put("from",messageSenderID);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef+"/"+messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverRef+"/"+messagePushID, messageTextBody);

            messageSender.child("LastTime").setValue(-date.getTime());
            messageReceiver.child("LastTime").setValue(-date.getTime());

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Toast.makeText(ChatActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                    MessageInputText.setText("");
                }
            });
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);




            if (resultCode == RESULT_OK) {

                loadingBar.setTitle("Send Image");
                loadingBar.setMessage("Please wait, your image is sending...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri resultUri = result.getUri();

                try {
                    compressedImage = new Compressor(this).setMaxWidth(200).setQuality(75).compressToFile(FileUtil.from(this, resultUri));
                } catch (Exception e) {
                }
                Uri resultUri2 = Uri.fromFile(new File(compressedImage.getAbsolutePath()));


                Date date = new Date();
                final String imageID =  Long.toString(date.getTime());

                filePath = userChatImgRef.child(imageID + ".jpg");

                saveImage(resultUri2, imageID);

                filePath.putFile(resultUri2).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Map messageImageBody = new HashMap();
                                    messageImageBody.put("image", uri.toString());
                                    messageImageBody.put("type", "image");
                                    messageImageBody.put("from", messageSenderID);
                                    messageImageBody.put("message", "[image]");
                                    messageImageBody.put("imageID",imageID);

                                    String messageSenderRef = "Messages/" + messageSenderID + "/" + messageReceiverID;
                                    String messageReceiverRef = "Messages/" + messageReceiverID + "/" + messageSenderID;

                                    DatabaseReference userImageKeyRef = RootRef.child("Messages")
                                            .child(messageSenderID).child(messageReceiverID).push();
                                    String messagePushID = userImageKeyRef.getKey();

                                    Map messageBodyDetails = new HashMap();
                                    messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageImageBody);
                                    messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageImageBody);
                                    Date date = new Date();
                                    messageSender = RootRef.child("Contacts").child(messageSenderID).child(messageReceiverID);
                                    messageReceiver = RootRef.child("Contacts").child(messageReceiverID).child(messageSenderID);
                                    messageSender.child("LastTime").setValue(-date.getTime());
                                    messageReceiver.child("LastTime").setValue(-date.getTime());

                                    RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {
                                            if(task.isSuccessful()){
                                                loadingBar.dismiss();
                                                Toast.makeText(ChatActivity.this, "Image Sent Successfully...", Toast.LENGTH_SHORT).show();
                                            }else{
                                                Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });

                                }


                            });

                        } else {
                            String message = task.getException().toString();
                            Toast.makeText(ChatActivity.this, "Error:" + message, Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });
            }
        }
    }

    private void saveImage(Uri resultUri2, String imageID) {

        File folder = new File(Environment.getExternalStorageDirectory() + "/Unichat/images/");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            String sourceFilename= resultUri2.getPath();
            String destinationFilename = android.os.Environment.getExternalStorageDirectory().getPath()+"/Unichat/images/"+imageID+".jpg";

            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;

            try {
                bis = new BufferedInputStream(new FileInputStream(sourceFilename));
                bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
                byte[] buf = new byte[1024];
                bis.read(buf);
                do {
                    bos.write(buf);
                } while(bis.read(buf) != -1);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bis != null) bis.close();
                    if (bos != null) bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            RootRef.child("Messages").child(messageSenderID).child(messageReceiverID).removeEventListener(eventListener);
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

}