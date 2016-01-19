package zhou.v2ex.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import zhou.v2ex.R;
import zhou.v2ex.data.DataManger;
import zhou.v2ex.data.RepliesProvider;
import zhou.v2ex.data.TopicProvider;
import zhou.v2ex.interfaces.OnItemClickListener;
import zhou.v2ex.interfaces.OnLoadCompleteListener;
import zhou.v2ex.model.Member;
import zhou.v2ex.model.Node;
import zhou.v2ex.model.Replies;
import zhou.v2ex.model.Topic;
import zhou.v2ex.ui.activity.MemberActivity;
import zhou.v2ex.ui.activity.NodeActivity;
import zhou.v2ex.ui.adapter.RepliesAdapter;
import zhou.v2ex.ui.widget.RichText;
import zhou.v2ex.util.ContentUtils;
import zhou.v2ex.util.TimeUtils;
import zhou.widget.AdvanceAdapter;

/**
 * Created by 州 on 2015/7/20 0020.
 * Topic详情
 */
public class TopicDetailFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ImageView icon;
    private TextView user, time, replay, node, title;
    private RichText content;
    private Topic topic;
    private RepliesAdapter repliesAdapter;
    private RepliesProvider repliesProvider;
    private TopicProvider topicProvider;
    private View detail;
    private AdvanceAdapter advanceAdapter;
    private CardView cardView;
    private OnLoadCompleteListener<Replies> onItemClickCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.containsKey(Topic.TOPIC)) {
            topic = bundle.getParcelable(Topic.TOPIC);
            repliesProvider = new RepliesProvider(DataManger.getInstance().getRestAdapter(), topic);
            topicProvider = new TopicProvider(DataManger.getInstance().getRestAdapter(), topic.id);
            DataManger.getInstance().addProvider(repliesProvider.FILE_NAME, repliesProvider);
            DataManger.getInstance().addProvider(topicProvider.FILE_NAME, topicProvider);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler_view, container, false);
        detail = inflater.inflate(R.layout.fragment_topic_detail, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view;
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        initView(detail);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        initData(topic);
        setUp(null);
        swipeRefreshLayout.setRefreshing(true);
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        refresh();

        return view;
    }

    private void initData(Topic topic) {
        if (topic == null) {
            return;
        }
        final Member member = topic.member;
        final Node n = topic.node;
        Picasso.with(getActivity()).load("http:" + member.avatar_normal).placeholder(R.drawable.default_image).into(icon);
        user.setText(member.username);
        time.setText(TimeUtils.friendlyFormat(topic.created * 1000));
        replay.setText(topic.replies + "个回复");
        node.setText(n.name);
        title.setText(topic.title);
        content.setRichText(ContentUtils.formatContent(topic.content_rendered));

        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MemberActivity.class);
                intent.putExtra(Member.MEMBER, (Parcelable) member);
                startActivity(intent);
            }
        });

        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), NodeActivity.class);
                intent.putExtra(Node.NODE, (Parcelable) n);
                startActivity(intent);
            }
        });
    }

    private void initView(View view) {
        icon = (ImageView) view.findViewById(R.id.topic_icon);
        user = (TextView) view.findViewById(R.id.topic_user);
        time = (TextView) view.findViewById(R.id.topic_time);
        replay = (TextView) view.findViewById(R.id.topic_replay);
        node = (TextView) view.findViewById(R.id.topic_node);
        title = (TextView) view.findViewById(R.id.topic_title);
        content = (RichText) view.findViewById(R.id.topic_content);
        cardView = (CardView) view.findViewById(R.id.topic_node_card);
    }

    private void setUp(List<Replies> replies) {
        repliesAdapter = new RepliesAdapter(replies);
        repliesAdapter.setIconClickCallback(onIconClickListener);
        repliesAdapter.setItemClickCallback(onItemClickListener);
        advanceAdapter = new AdvanceAdapter(repliesAdapter);
        advanceAdapter.addHeader(detail);
        recyclerView.setAdapter(advanceAdapter);
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refresh();
        }
    };

    private OnLoadCompleteListener<List<Replies>> onLoadComplete = new OnLoadCompleteListener<List<Replies>>() {
        @Override
        public void loadComplete(List<Replies> replies) {
            if (replies != null) {
                repliesAdapter.setReplies(replies);
                Log.i("complete", "size:" + replies.size());
            }
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    private OnLoadCompleteListener<Topic> topicOnLoadComplete = new OnLoadCompleteListener<Topic>() {
        @Override
        public void loadComplete(Topic topic) {
            initData(topic);
        }
    };

    private OnItemClickListener onIconClickListener = new OnItemClickListener() {
        @Override
        public void onItemClicked(View view, int position) {
            Replies reply = repliesAdapter.getItem(position - advanceAdapter.getHeaderSize());
            Intent intent = new Intent(getActivity(), MemberActivity.class);
            intent.putExtra(Member.MEMBER, (Parcelable) reply.member);
            startActivity(intent);
        }
    };
    private OnItemClickListener onItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClicked(View view, int position) {
            Replies reply = repliesAdapter.getItem(position - advanceAdapter.getHeaderSize());
            if (onItemClickCallback != null) {
                onItemClickCallback.loadComplete(reply);
            }
        }
    };

    public void refresh() {
        DataManger.getInstance().refresh(repliesProvider.FILE_NAME, onLoadComplete);
        DataManger.getInstance().refresh(topicProvider.FILE_NAME, topicOnLoadComplete);
    }

    public void scrollToBottom() {
        recyclerView.scrollToPosition(advanceAdapter.getItemCount() - 1);
    }

    @SuppressWarnings("unused")
    public void scrollToTop() {
        recyclerView.scrollToPosition(0);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        DataManger.getInstance().removeProvider(topicProvider.FILE_NAME);
    }

    public static TopicDetailFragment newInstance(@NonNull Topic topic) {
        TopicDetailFragment topicDetailFragment = new TopicDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(Topic.TOPIC, topic);
        topicDetailFragment.setArguments(bundle);
        return topicDetailFragment;
    }


    public void setOnItemClickCallback(OnLoadCompleteListener<Replies> onItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback;
    }
}
