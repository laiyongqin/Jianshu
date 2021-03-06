package com.tongming.jianshu.fragment;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bigkoo.convenientbanner.ConvenientBanner;
import com.bigkoo.convenientbanner.holder.CBViewHolderCreator;
import com.bigkoo.convenientbanner.holder.Holder;
import com.bumptech.glide.Glide;
import com.tongming.jianshu.R;
import com.tongming.jianshu.activity.ArticleDetailActivity;
import com.tongming.jianshu.adapter.ArticleRecylerViewAdapter;
import com.tongming.jianshu.adapter.HeaderAndFooterRecyclerViewAdapter;
import com.tongming.jianshu.adapter.onRecyclerViewItemClickListener;
import com.tongming.jianshu.base.BaseFragment;
import com.tongming.jianshu.bean.Article;
import com.tongming.jianshu.bean.ArticleList;
import com.tongming.jianshu.presenter.ArticlePresenterCompl;
import com.tongming.jianshu.util.LogUtil;
import com.tongming.jianshu.util.RecyclerViewUtil;
import com.tongming.jianshu.util.ToastUtil;
import com.tongming.jianshu.view.RecyclerViewDivider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Tongming on 2016/5/21.
 */
public class ArticleFragment extends BaseFragment implements IArticleView {

    private static final String TAG = "Article";
    private boolean flag = false;

    @BindView(R.id.hot_swipe)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.rv_hot)
    RecyclerView recyclerView;
    private ArticleRecylerViewAdapter adapter;
    private int type;
    private ArticlePresenterCompl compl;
    private List<Article> articleList;
    private LinearLayoutManager layoutManager;
    private HeaderAndFooterRecyclerViewAdapter mAdapter;
    private LinearLayout footer;

    public static ArticleFragment newInstance(int type) {
        ArticleFragment articleFragment = new ArticleFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        articleFragment.setArguments(bundle);
        return articleFragment;
    }

    @Override
    protected void initViews() {
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new RecyclerViewDivider(
                getActivity(), LinearLayoutManager.VERTICAL, 3, getResources().getColor(R.color.divide_gray)
        ));
        refreshLayout.setColorSchemeResources(new int[]{R.color.colorPrimary});
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                type = getArguments().getInt("type");
                compl.getArticleList(type + "");
            }
        });
        footer = (LinearLayout) View.inflate(getActivity(), R.layout.item_footer, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        WindowManager manager = getActivity().getWindowManager();
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        params.leftMargin = metrics.widthPixels / 2 - 79;  //footer的宽度为158
        footer.setLayoutParams(params);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_article;
    }

    @Override
    protected void afterCreate(Bundle saveInstanceState) {
        /*isPrepared = true;
        lazyLoad();*/
        articleList = new ArrayList<>();
    }

    @Override
    protected void lazyLoad() {
        if (!isPrepared || !isVisible) {
            return;
        }
        if (!flag) {
            refreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    refreshLayout.setRefreshing(true);
                }
            });
            compl = new ArticlePresenterCompl(this);
            type = getArguments().getInt("type");
            compl.getArticleList(type + "");
            flag = true;
        }
    }

    @Override
    public void onGetArticle(final ArticleList list) {
        if (articleList.size() == 0) {
            articleList.addAll(list.getResults());
        } else {
            articleList.clear();
            articleList.addAll(list.getResults());
        }
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && layoutManager.findLastCompletelyVisibleItemPosition() + 1 == mAdapter.getItemCount()) {
                    boolean isRefreshing = refreshLayout.isRefreshing();
                    if (isRefreshing) {
                        adapter.notifyItemRemoved(adapter.getItemCount());
                        return;
                    } else {
                        //滑动到底部时触发加载更多数据的操作
                        compl.loadMore(list.getIds(), list.getPage(), type);
                    }
                }
            }
        });

        if (adapter == null) {
            adapter = new ArticleRecylerViewAdapter(getActivity(), articleList);
            //点击文章的事件监听
            adapter.setOnItemClickListener(new onRecyclerViewItemClickListener() {
                @Override
                public void onItemClick(View view, String slug) {
                    Intent intent = new Intent(getActivity(), ArticleDetailActivity.class);
                    intent.putExtra("slug", slug);
                    startActivity(intent,
                            ActivityOptions.makeSceneTransitionAnimation(getActivity()).toBundle());
                }
            });
        } else {
            adapter.notifyDataSetChanged();
            ToastUtil.showToast(getActivity(), "刷新完成");
        }
        mAdapter = new HeaderAndFooterRecyclerViewAdapter(adapter);
        recyclerView.setAdapter(mAdapter);
        if (type == 0) {
            //添加header
            final ConvenientBanner banner = (ConvenientBanner) View.inflate(getActivity(), R.layout.item_banner, null);
            banner.setPages(new CBViewHolderCreator() {
                @Override
                public Object createHolder() {
                    return new Holder<String>() {
                        private ImageView imageView;

                        @Override
                        public View createView(Context context) {
                            imageView = new ImageView(context);
                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            return imageView;
                        }

                        @Override
                        public void UpdateUI(Context context, int position, String data) {
                            Glide.with(context).load(data).into(imageView);
                        }
                    };
                }
            }, list.getBanner()).setPageIndicator(new int[]{R.drawable.point_bg_normal, R.drawable.point_bg_enable})
                    .setPageIndicatorAlign(ConvenientBanner.PageIndicatorAlign.CENTER_HORIZONTAL);
//            banner.setManualPageable(true);   //这TM就是个坑,是手动的- -,英语不好怪我咯
            banner.startTurning(5000);
            final float scale = getActivity().getResources().getDisplayMetrics().density;
            int height = (int) (180 * scale + 0.5f);
            banner.setLayoutParams(new LinearLayoutCompat.LayoutParams(getActivity().getWindowManager().getDefaultDisplay().getWidth(), height));
            RecyclerViewUtil.setHeaderView(recyclerView, banner);
        }
        RecyclerViewUtil.setFooterView(recyclerView, footer);
    }

    @Override
    public void onFailed() {
        ToastUtil.showToast(getActivity(), "请求出错");
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                ToastUtil.showToast(getActivity(), "正在重新请求数据");
                compl.getArticleList(type + "");
            }
        });
    }

    //下拉加载,数据获取完成之后的操作
    @Override
    public void onLoadMore(ArticleList list) {
        List<Article> resultsBeanList = list.getResults();
        articleList.addAll(resultsBeanList);
//        resultsBeanList.addAll(articleList);
        adapter.notifyDataSetChanged();
        LogUtil.d(TAG, adapter.getList().size() + "");
    }


}
