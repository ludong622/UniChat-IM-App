package comp5216.sydney.edu.au.unichat;

import android.content.Context;
import android.content.Intent;
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
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class GroupChatActivity extends AppCompatActivity {

    private String otherName, currentID, groupName, groupKey,groupType;
    private TextView groupNameTextView;

    private Toolbar GroupToolBar;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef,GroupMessageKeyRef,GroupRef;

    private ImageButton SendMessageButton;
    private EditText MessageInputText;

    private final List<GroupMessages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private GroupMessageAdapter messageAdapter;
    private RecyclerView userMessagesList;
    private String fromName;
    private Button addNewBtn;
    private ChildEventListener eventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        mAuth = FirebaseAuth.getInstance();
        currentID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();


        groupName=getIntent().getExtras().get("groupName").toString();
        groupKey=getIntent().getExtras().get("groupKey").toString();

        groupType=getIntent().getExtras().get("groupType").toString();



        if(groupType.equals("normal")){
            GroupRef=RootRef.child("Groups");
        }else{
            GroupRef=RootRef.child("CourseGroups");
        }



        RootRef.child("Users").child(currentID).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    fromName=dataSnapshot.getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



        IntializeControllers();



        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();
            }
        });
        addNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editGroupIntent = new Intent(GroupChatActivity.this,CreateGroupActivity.class);
                editGroupIntent.putExtra("GroupKey",groupKey);
                editGroupIntent.putExtra("GroupName",groupName);
                startActivity(editGroupIntent);
            }
        });

        eventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                GroupMessages messages = dataSnapshot.getValue(GroupMessages.class);
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
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void IntializeControllers() {

        GroupToolBar =(Toolbar) findViewById(R.id.group_chat_toolbar);
        setSupportActionBar(GroupToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(groupName);


        groupNameTextView = (TextView) findViewById(R.id.custom_profile_name);

        SendMessageButton=(ImageButton)findViewById(R.id.group_send_message_btn);
        MessageInputText = (EditText)findViewById(R.id.group_input_message);

        messageAdapter = new GroupMessageAdapter(messagesList);
        userMessagesList = (RecyclerView)findViewById(R.id.group_private_messages_list_of_users);
        addNewBtn=findViewById(R.id.group_chat_add_memebers);
        if(groupType.equals("normal")){
            addNewBtn.setVisibility(View.VISIBLE);
        }

        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        messagesList.clear();


        GroupRef.child(groupKey).child("messages").addChildEventListener(eventListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        messagesList.clear();
        GroupRef.child(groupKey).child("messages").removeEventListener(eventListener);
    }

    private void SendMessage(){
        String message = MessageInputText.getText().toString();
        String messageKey = GroupRef.child(groupKey).child("messages").push().getKey();
        if(TextUtils.isEmpty(message)){
            Toast.makeText(this, "Please write message first...", Toast.LENGTH_SHORT).show();
        }else{

            HashMap<String,Object> groupMessageKey = new HashMap<>();
            GroupRef.child(groupName).updateChildren(groupMessageKey);

            GroupMessageKeyRef = GroupRef.child(groupKey).child("messages").child(messageKey);

            HashMap<String,Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("fromName",fromName);
            messageInfoMap.put("message",message);
            messageInfoMap.put("fromID",currentID);

            GroupMessageKeyRef.updateChildren(messageInfoMap);
        }
        MessageInputText.setText("");

    }

    public void backClick(View v){
        finish();
    }
}