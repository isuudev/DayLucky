package com.zaotao.daylucky.presenter;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.widget.ImageView;

import com.zaotao.base.rx.RxBus;
import com.zaotao.base.rx.RxBusSubscriber;
import com.zaotao.base.rx.RxSchedulers;
import com.zaotao.daylucky.app.ColorManager;
import com.zaotao.daylucky.app.DateUtils;
import com.zaotao.daylucky.app.LocalDataManager;
import com.zaotao.daylucky.base.BasePresenter;
import com.zaotao.daylucky.contract.MainHomeContract;
import com.zaotao.daylucky.module.api.ApiNetwork;
import com.zaotao.daylucky.module.api.ApiService;
import com.zaotao.daylucky.module.api.ApiSubscriber;
import com.zaotao.daylucky.module.api.BaseResult;
import com.zaotao.daylucky.module.entity.LuckyEntity;
import com.zaotao.daylucky.module.entity.ThemeEntity;
import com.zaotao.daylucky.module.event.SelectEvent;

import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class MainHomePresenter extends BasePresenter<MainHomeContract.View> implements MainHomeContract.Presenter {

    private static final String TAG = "MainHomePresenter";

    private ApiService apiService;

    @Override
    public void onPresenterCreated() {
        apiService = ApiNetwork.getNetService(ApiService.class);
    }

    @Override
    public void onPresenterDestroy() {
    }

    @Override
    public void registerSelectPosition(ImageView imageView) {
        RxBus.getDefault().toObservable(SelectEvent.class)
                .compose(RxSchedulers.applySchedulers(getLifecycleProvider()))
                .subscribe(new RxBusSubscriber<SelectEvent>() {
                    @Override
                    public void onEvent(SelectEvent selectEvent) {
                        initHomeLucky(selectEvent.getVar());
                        imageView.setImageResource(LocalDataManager.getInstance().getImageRes());
                    }

                    @Override
                    public void onFailure(String errMsg) {

                    }
                });
    }

    @Override
    public void registerThemeInfo() {
        RxBus.getDefault().toObservable(ThemeEntity.class)
                .compose(RxSchedulers.applySchedulers(getLifecycleProvider()))
                .subscribe(new RxBusSubscriber<ThemeEntity>() {
                    @Override
                    public void onEvent(ThemeEntity themeEntity) {
                        getView().onSuccessThemeInfo(themeEntity);
                    }

                    @Override
                    public void onFailure(String errMsg) {

                    }
                });
    }

    @Override
    public void initHomeLucky(int position) {
        apiService.initHomeLucky(position)
                .filter(new Predicate<BaseResult<LuckyEntity>>() {
                    @Override
                    public boolean test(BaseResult<LuckyEntity> luckyEntityResult) throws Exception {
                        return luckyEntityResult.success();
                    }
                })
                .map(new Function<BaseResult<LuckyEntity>, LuckyEntity>() {
                    @Override
                    public LuckyEntity apply(BaseResult<LuckyEntity> luckyEntityResult) throws Exception {
                        return luckyEntityResult.getData();
                    }
                })
                .compose(RxSchedulers.applySchedulers(getLifecycleProvider()))
                .subscribe(new ApiSubscriber<LuckyEntity>() {
                    @Override
                    public void onSuccess(LuckyEntity luckyEntity) {
                        ThemeEntity themeEntity = new ThemeEntity();
                        themeEntity.setBad(luckyEntity.getToday().getBad());
                        themeEntity.setLucky(luckyEntity.getToday().getLuck());
                        themeEntity.setText(luckyEntity.getToday().getCont());
                        themeEntity.setDay(DateUtils.formatDayText());
                        themeEntity.setMonth(DateUtils.formatMonthText());
                        themeEntity.setWeek(DateUtils.formatWeekText());
                        themeEntity.setLineColor(ColorManager.colorsLineBg[0][0]);
                        themeEntity.setBgColor(ColorManager.colorsLineBg[0][1]);

                        if (LocalDataManager.getInstance().isEmptyLocalTheme()) {
                            RxBus.getDefault().post(themeEntity);
                        }else {
                            RxBus.getDefault().post(LocalDataManager.getInstance().getThemeData());
                        }
                        getView().onSuccessLucky(luckyEntity);
                    }

                    @Override
                    public void onFailure(String errMsg) {

                    }
                });
    }
}