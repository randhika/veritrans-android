package id.co.veritrans.sdk.uiflow.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.IntegerRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import id.co.veritrans.sdk.coreflow.core.Constants;
import id.co.veritrans.sdk.coreflow.core.Logger;
import id.co.veritrans.sdk.coreflow.core.VeritransSDK;
import id.co.veritrans.sdk.coreflow.eventbus.bus.VeritransBusProvider;
import id.co.veritrans.sdk.coreflow.eventbus.callback.CardRegistrationBusCallback;
import id.co.veritrans.sdk.coreflow.eventbus.callback.GetCardBusCallback;
import id.co.veritrans.sdk.coreflow.eventbus.callback.SaveCardBusCallback;
import id.co.veritrans.sdk.coreflow.eventbus.events.CardRegistrationFailedEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.CardRegistrationSuccessEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.GeneralErrorEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.GetCardFailedEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.GetCardsSuccessEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.NetworkUnavailableEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.SaveCardFailedEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.SaveCardSuccessEvent;
import id.co.veritrans.sdk.coreflow.eventbus.events.UpdateCreditCardDataFromScanEvent;
import id.co.veritrans.sdk.coreflow.models.CardResponse;
import id.co.veritrans.sdk.coreflow.models.SaveCardRequest;
import id.co.veritrans.sdk.coreflow.utilities.Utils;
import id.co.veritrans.sdk.uiflow.R;
import id.co.veritrans.sdk.uiflow.adapters.RegisterCardPagerAdapter;
import id.co.veritrans.sdk.uiflow.fragments.PaymentTransactionStatusFragment;
import id.co.veritrans.sdk.uiflow.fragments.RegisterSavedCardFragment;
import id.co.veritrans.sdk.uiflow.scancard.ExternalScanner;
import id.co.veritrans.sdk.uiflow.scancard.ScannerModel;
import id.co.veritrans.sdk.uiflow.utilities.SdkUIFlowUtil;
import id.co.veritrans.sdk.uiflow.widgets.CirclePageIndicator;
import id.co.veritrans.sdk.uiflow.widgets.MorphingButton;

/**
 * @author rakawm
 */
public class SaveCreditCardActivity extends BaseActivity implements SaveCardBusCallback, CardRegistrationBusCallback, GetCardBusCallback {
    public static final int SCAN_REQUEST_CODE = 101;
    private int RESULT_CODE = RESULT_CANCELED;
    private Toolbar toolbar;
    private ImageView logo;
    private VeritransSDK veritransSDK;
    private RelativeLayout processingLayout;
    private String currentFragmentName;
    private float cardWidth;
    private FragmentManager fragmentManager;
    private LinearLayout emptyContainer;
    private TextView titleHeaderTextView;
    private int fabHeight;
    private MorphingButton btnMorph;
    private CirclePageIndicator circlePageIndicator;
    private RegisterCardPagerAdapter pagerAdapter;
    private ArrayList<SaveCardRequest> creditCards = new ArrayList<>();
    private RegisterSavedCardFragment savedCardFragment;
    private SaveCardRequest cardRequest;

    public MorphingButton getBtnMorph() {
        return btnMorph;
    }

    public TextView getTitleHeaderTextView() {
        return titleHeaderTextView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VeritransBusProvider.getInstance().isRegistered(this)) {
            VeritransBusProvider.getInstance().register(this);
        }
        setContentView(R.layout.activity_save_credit_card);

        processingLayout = (RelativeLayout) findViewById(R.id.processing_layout);
        veritransSDK = VeritransSDK.getVeritransSDK();
        fragmentManager = getSupportFragmentManager();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        titleHeaderTextView = (TextView) findViewById(R.id.title_header);
        btnMorph = (MorphingButton) findViewById(R.id.btnMorph1);
        logo = (ImageView) findViewById(R.id.merchant_logo);
        initializeTheme();

        morphToCircle(0);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        calculateScreenWidth();
        getCreditCards();

        savedCardFragment = RegisterSavedCardFragment.newInstance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (VeritransBusProvider.getInstance().isRegistered(this)) {
            VeritransBusProvider.getInstance().unregister(this);
        }
    }

    public void morphToCircle(int time) {
        MorphingButton.Params circle = morphCicle(time);
        btnMorph.morph(circle);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.i("reqCode:" + requestCode + ",res:" + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == SCAN_REQUEST_CODE) {
                if (data != null && data.hasExtra(ExternalScanner.EXTRA_SCAN_DATA)) {
                    ScannerModel scanData = (ScannerModel) data.getSerializableExtra(ExternalScanner.EXTRA_SCAN_DATA);
                    Logger.i(String.format("Card Number: %s, Card Expire: %s/%d", scanData.getCardNumber(), scanData.getExpiredMonth() < 10 ? String.format("0%d", scanData.getExpiredMonth()) : String.format("%d", scanData.getExpiredMonth()), scanData.getExpiredYear() - 2000));
                    updateCreditCardData(Utils.getFormattedCreditCardNumber(scanData.getCardNumber()), scanData.getCvv(), String.format("%s/%d", scanData.getExpiredMonth() < 10 ? String.format("0%d", scanData.getExpiredMonth()) : String.format("%d", scanData.getExpiredMonth()), scanData.getExpiredYear() - 2000));
                } else {
                    Logger.d("No result");
                }
            }
        }
    }

    private void updateCreditCardData(String cardNumber, String cvv, String expired) {
        // Update credit card data in AddCardDetailsFragment
        VeritransBusProvider.getInstance().post(new UpdateCreditCardDataFromScanEvent(cardNumber, cvv, expired));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        SdkUIFlowUtil.hideKeyboard(this);
        if (fragmentManager.getBackStackEntryCount() == 1) {
            setResultAndFinish();
        } else {
            if (!TextUtils.isEmpty(currentFragmentName) && currentFragmentName.equalsIgnoreCase(PaymentTransactionStatusFragment.class
                    .getName())) {
                setResultAndFinish();
            } else {

                super.onBackPressed();
            }
        }
    }

    public VeritransSDK getVeritransSDK() {
        return veritransSDK;
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    private void calculateScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;
        cardWidth = outMetrics.widthPixels;
        cardWidth = cardWidth - ((2 * getResources().getDimension(R.dimen.sixteen_dp)) / density);
    }

    public float getScreenWidth() {
        return cardWidth;
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack, boolean clearBackStack) {
        if (fragment != null) {
            Logger.i("replace freagment");
            boolean fragmentPopped = false;
            String backStateName = fragment.getClass().getName();

            if (clearBackStack) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                fragmentPopped = fragmentManager.popBackStackImmediate(backStateName, 0);
            }

            if (!fragmentPopped) { //fragment not in back stack, create it.
                Logger.i("fragment not in back stack, create it");
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.card_container, fragment, backStateName);
                if (addToBackStack) {
                    ft.addToBackStack(backStateName);
                }
                ft.commit();
                currentFragmentName = backStateName;
                //currentFragment = fragment;
            }
        }
    }

    public void registerCard(String cardNumber, String cvv, String expMonth, String expYear) {
        veritransSDK.cardRegistration(cardNumber, cvv, expMonth, expYear);
    }

    public void saveCreditCards(SaveCardRequest creditCard) {
        veritransSDK.saveCards(creditCard);
    }

    public ArrayList<SaveCardRequest> getCreditCards() {
        if (creditCards == null || creditCards.isEmpty()) {
            fetchCreditCards();
        }
        return creditCards;
    }

    public ArrayList<SaveCardRequest> getCreditCardList() {
        return creditCards;
    }

    public void fetchCreditCards() {
        SdkUIFlowUtil.showProgressDialog(this, getString(R.string.fetching_cards), true);
        //  processingLayout.setVisibility(View.VISIBLE);
        veritransSDK.getSavedCard();
    }

    public int getFabHeight() {
        return fabHeight;
    }

    public void setFabHeight(int fabHeight) {
        Logger.i("fab_height:" + fabHeight);
        this.fabHeight = fabHeight;
    }

    public void morphingAnimation() {
        Logger.i("morphingAnimation");
        //Logger.i("64dp:"+ Utils.dpToPx(56));
        btnMorph.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MorphingButton.Params square = MorphingButton.Params.create()
                        .duration((int) Constants.CARD_ANIMATION_TIME)
                        .cornerRadius(dimen(R.dimen.mb_corner_radius_2))
                        .width((int) cardWidth)
                        .height(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                        .text(getString(R.string.pay_now))
                        .colorPressed(color(R.color.colorAccent))
                        .color(color(R.color.colorAccent));
                btnMorph.morph(square);
            }
        }, 50);

    }

    public void setResultAndFinish() {
        Intent data = new Intent();
        setResult(RESULT_CODE, data);
        finish();
    }

    public MorphingButton.Params morphCicle(int time) {
        return MorphingButton.Params.create()
                .cornerRadius(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                .width(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                .height(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                .duration(time)
                .colorPressed(color(R.color.colorAccent))
                .color(color(R.color.colorAccent));


    }

    public void setAdapterViews(RegisterCardPagerAdapter cardPagerAdapter, CirclePageIndicator circlePageIndicator, LinearLayout emptyContainer) {
        this.pagerAdapter = cardPagerAdapter;
        this.circlePageIndicator = circlePageIndicator;
        this.emptyContainer = emptyContainer;
    }

    public int dimen(@DimenRes int resId) {
        return (int) getResources().getDimension(resId);
    }

    public int color(@ColorRes int resId) {
        return getResources().getColor(resId);
    }

    public int integer(@IntegerRes int resId) {
        return getResources().getInteger(resId);
    }

    @Subscribe
    @Override
    public void onEvent(CardRegistrationSuccessEvent event) {
        cardRequest = new SaveCardRequest();
        cardRequest.setCode(event.getResponse().getStatusCode());
        cardRequest.setMaskedCard(event.getResponse().getMaskedCard());
        cardRequest.setSavedTokenId(event.getResponse().getSavedTokenId());
        cardRequest.setTransactionId(event.getResponse().getTransactionId());
        veritransSDK.saveCards(cardRequest);
    }

    @Subscribe
    @Override
    public void onEvent(CardRegistrationFailedEvent event) {
        SdkUIFlowUtil.hideProgressDialog();
        SdkUIFlowUtil.showApiFailedMessage(this, event.getMessage());
    }

    @Subscribe
    @Override
    public void onEvent(GetCardsSuccessEvent event) {
        CardResponse cardResponse = event.getResponse();
        SdkUIFlowUtil.hideProgressDialog();
        //
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processingLayout.setVisibility(View.GONE);
            }
        }, 200);
        Logger.i("cards api successful" + cardResponse);
        if (cardResponse != null) {
            creditCards.clear();
            creditCards.addAll(cardResponse.getData());
            if (pagerAdapter != null && circlePageIndicator != null) {
                circlePageIndicator.notifyDataSetChanged();
            }
            if (emptyContainer != null) {
                if (!creditCards.isEmpty()) {
                    emptyContainer.setVisibility(View.GONE);
                } else {
                    emptyContainer.setVisibility(View.VISIBLE);
                }

            }
            titleHeaderTextView.setText(getString(R.string.saved_card));
            replaceFragment(savedCardFragment, true, false);

        }
    }

    @Subscribe
    @Override
    public void onEvent(GetCardFailedEvent event) {
        SdkUIFlowUtil.hideProgressDialog();
        Logger.i("card fetching failed :" + event.getMessage());
        processingLayout.setVisibility(View.GONE);
    }

    @Subscribe
    @Override
    public void onEvent(NetworkUnavailableEvent event) {
        SdkUIFlowUtil.hideProgressDialog();
        SdkUIFlowUtil.showApiFailedMessage(this, getString(R.string.no_network_msg));
    }

    @Subscribe
    @Override
    public void onEvent(GeneralErrorEvent event) {
        SdkUIFlowUtil.hideProgressDialog();
        SdkUIFlowUtil.showApiFailedMessage(this, event.getMessage());
    }

    @Subscribe
    @Override
    public void onEvent(SaveCardSuccessEvent event) {
        SdkUIFlowUtil.hideProgressDialog();
        SdkUIFlowUtil.showSnackbar(this, "Your card has been successfully saved");
        savedCardFragment.addCreditCard(cardRequest);
        replaceFragment(savedCardFragment, true, false);
    }

    @Subscribe
    @Override
    public void onEvent(SaveCardFailedEvent event) {
        SdkUIFlowUtil.hideProgressDialog();
        SdkUIFlowUtil.showApiFailedMessage(this, event.getMessage());
    }

}
