package cn.ucai.live.ui.activity;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hyphenate.EMChatRoomChangeListener;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCursorResult;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.exceptions.HyphenateException;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ucai.live.R;
import cn.ucai.live.data.model.LiveRoom;
import cn.ucai.live.ui.GridMarginDecoration;
import cn.ucai.live.utils.L;
import cn.ucai.live.utils.PreferenceManager;

/**
 * A simple {@link Fragment} subclass.
 */
public class LiveListFragment extends Fragment {
    private static final String TAG = LiveListFragment.class.getSimpleName();
    private List<EMChatRoom> chatRoomList;
    private LiveAdapter adapter;
    private boolean isLoading;
    private boolean isFirstLoading = true;
    private boolean hasMoreData = true;
    private String cursor;
    private final int pagesize = 4;
    private LinearLayout footLoadingLayout;
    private ProgressBar footLoadingPB;
    private TextView footLoadingText;
    private EditText etSearch;
    private ImageButton ibClean;
    private List<EMChatRoom> rooms;
    RecyclerView recyclerView;
    GridLayoutManager gm;
    ProgressDialog pd;
    SwipeRefreshLayout mRefreshLayout;
    TextView mRefreshHint;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_live_list, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        chatRoomList = new ArrayList<EMChatRoom>();
        rooms = new ArrayList<EMChatRoom>();
//        adapter = new LiveAdapter(getContext(), getLiveRoomList(rooms));
        pd = new ProgressDialog(getContext());
        pd.show();
        recyclerView = (RecyclerView) getView().findViewById(R.id.recycleview);
//        View footView = getLayoutInflater().inflate(R.layout.em_listview_footer_view, gm, false);
//        GridLayoutManager glm = (GridLayoutManager) recyclerView.getLayoutManager();
        gm = new GridLayoutManager(getActivity(), 2);

        recyclerView.setLayoutManager(gm);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new GridMarginDecoration(6));
//        recyclerView.setAdapter(adapter);
//        recyclerView.setAdapter(new LiveAdapter(getActivity(), TestDataRepository.getLiveRoomList()));
        mRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.mRefreshLayout);
        mRefreshLayout.setColorSchemeColors(Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW);

        mRefreshHint = (TextView) getView().findViewById(R.id.mRefreshHint);
        footLoadingLayout = (LinearLayout) getView().findViewById(R.id.loading_layout);
        footLoadingPB = (ProgressBar) getView().findViewById(R.id.loading_bar);
        footLoadingText = (TextView) getView().findViewById(R.id.loading_text);
//        listView.addFooterView(getView(), null, false);
        footLoadingLayout.setVisibility(View.GONE);

        loadAndShowData();//加载并显示数据
        setListener();
    }

    private void setListener() {
        EMClient.getInstance().chatroomManager().addChatRoomChangeListener(new EMChatRoomChangeListener() {
            @Override
            public void onChatRoomDestroyed(String roomId, String roomName) {
                chatRoomList.clear();
                if (adapter != null) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (adapter != null) {
                                adapter.notifyDataSetChanged();
                                loadAndShowData();
                            }
                        }

                    });
                }
            }

            @Override
            public void onMemberJoined(String s, String s1) {

            }

            @Override
            public void onMemberExited(String s, String s1, String s2) {

            }

            @Override
            public void onMemberKicked(String s, String s1, String s2) {

            }
        });
//        上拉加载
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
//                        int lasPos = gm.getLastVisiblePosition();
                    int lasPos = gm.findLastVisibleItemPosition();
//                        if(hasMoreData && !isLoading && lasPos == ListView.getCount()-1){
                    if (hasMoreData && !isLoading && lasPos == chatRoomList.size() - 1) {
                        loadAndShowData();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstPostion = gm.findFirstVisibleItemPosition();//可见的第一行，判断是否为第一行，
                mRefreshLayout.setEnabled(firstPostion == 0);//来判断*/
//                这里可以解决刷新之后，网络请求不到数据，我们可以设置
            }
        });
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.setRefreshing(true);//转动东西开始
                mRefreshLayout.setEnabled(true);
                mRefreshHint.setVisibility(View.VISIBLE);
                cursor = null;
                chatRoomList.clear();
                //开始时已经加载过，但注意这里这样写会造成recyclerView.setAdapter(adapter);重复
//                isFirstLoading = true;
                loadAndShowData();
            }
        });
    }

    private void loadAndShowData() {
//        下载需要新开子线程是，最好不要在主线程
        new Thread(new Runnable() {
            public void run() {
                try {
                    isLoading = true;
//                    pagenum += 1;
//                    fetchPublicChatRoomsFromServer(pagesize, cursor);首次为null，默认加载pagesize条数据，后面不是
                    final EMCursorResult<EMChatRoom> result = EMClient.getInstance().chatroomManager().fetchPublicChatRoomsFromServer(pagesize, cursor);
                    final List<EMChatRoom> chatRooms = result.getData();
                    L.e(TAG, "" + chatRooms.size());
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
//                            groupsList.addAll(returnGroups);
                            mRefreshLayout.setRefreshing(false);
                            mRefreshHint.setVisibility(View.GONE);
                            chatRoomList.addAll(chatRooms);
                            if (chatRooms.size() != 0) {
                                cursor = result.getCursor();
                                if (chatRooms.size() == pagesize)
                                    footLoadingLayout.setVisibility(View.VISIBLE);
                            }
                            if (isFirstLoading) {
//                                pb.setVisibility(View.INVISIBLE);
//                                第一次加载，adapter，这样就不要在上面定义了
                                pd.dismiss();
                                isFirstLoading = false;
                                adapter = new LiveAdapter(getContext(), getLiveRoomList(chatRoomList));
                                recyclerView.setAdapter(adapter);//重复了，
                            } else {
                                if (chatRooms.size() < pagesize) {
                                    pd.dismiss();
                                    hasMoreData = false;
                                    footLoadingLayout.setVisibility(View.VISIBLE);
                                    footLoadingPB.setVisibility(View.GONE);
                                    footLoadingText.setText("No more data");
                                }
                                adapter.notifyDataSetChanged();
                            }
                            isLoading = false;
                        }
                    });
                } catch (HyphenateException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            isLoading = false;
                            mRefreshLayout.setRefreshing(false);
                            mRefreshHint.setVisibility(View.GONE);
//                            pb.setVisibility(View.INVISIBLE);
                            pd.dismiss();
                            footLoadingLayout.setVisibility(View.GONE);
                            Toast.makeText(getContext(), getResources().getString(R.string.failed_to_load_data), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 群聊组转化成直播间
     */
    public static List<LiveRoom> getLiveRoomList(List<EMChatRoom> chatRooms) {
        List<LiveRoom> roomList = new ArrayList<>();
        for (EMChatRoom room : chatRooms) {
            LiveRoom liveRoom = new LiveRoom();
            liveRoom.setName(room.getName());
            liveRoom.setAudienceNum(room.getAffiliationsCount());
            liveRoom.setId(room.getId());
            liveRoom.setChatroomId(room.getId());
//            获取头像，
//            liveRoom.setCover(R.drawable.test1);
//            liveRoom.setCover(EaseUserUtils.getAppUserInfo(PreferenceManager.getInstance().getCurrentUsername()).getAvatar());
            liveRoom.setCover(EaseUserUtils.setAppAvatarByName(room.getOwner()));
            liveRoom.setAnchorId(room.getOwner());
            L.e(TAG, "liveRoom" + liveRoom.toString());
            roomList.add(liveRoom);
        }

        return roomList;
    }

    static class LiveAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

        private final List<LiveRoom> liveRoomList;
        private final Context context;

        public LiveAdapter(Context context, List<LiveRoom> liveRoomList) {
            this.liveRoomList = liveRoomList;
            this.context = context;
        }

        @Override
        public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final PhotoViewHolder holder = new PhotoViewHolder(LayoutInflater.from(context).
                    inflate(R.layout.layout_livelist_item, parent, false));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = holder.getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) return;
                    LiveRoom room = liveRoomList.get(position);
                    if (room.getAnchorId().equals(EMClient.getInstance().getCurrentUser())) {
                        context.startActivity(new Intent(context, StartLiveActivity.class)
                                .putExtra("liveId", room.getId()));
                    } else {
//                        不是自己，进入别人的直播间
                        context.startActivity(new Intent(context, LiveDetailsActivity.class)
                                .putExtra("liveroom", liveRoomList.get(position)));
                    }
                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(PhotoViewHolder holder, int position) {
            LiveRoom liveRoom = liveRoomList.get(position);
            holder.anchor.setText(liveRoom.getName());
            holder.audienceNum.setText(liveRoom.getAudienceNum() + "人");
            if (liveRoomList.get(position).getAnchorId().equals(EMClient.getInstance().getCurrentUser())){
                EaseUserUtils.setAppUserAvatar(context, PreferenceManager.getInstance().getCurrentUsername(),holder.imageView);
            }else{
                Glide.with(context)
                        .load(liveRoomList.get(position).getCover())
                        .placeholder(R.color.placeholder)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return liveRoomList.size();
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.photo)
        ImageView imageView;
        @BindView(R.id.author)
        TextView anchor;
        @BindView(R.id.audience_num)
        TextView audienceNum;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
