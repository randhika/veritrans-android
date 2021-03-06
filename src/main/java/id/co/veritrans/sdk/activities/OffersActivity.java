package id.co.veritrans.sdk.activities;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import id.co.veritrans.sdk.R;
import id.co.veritrans.sdk.adapters.CardPagerAdapter;
import id.co.veritrans.sdk.core.Constants;
import id.co.veritrans.sdk.core.ExternalScanner;
import id.co.veritrans.sdk.core.LocalDataHandler;
import id.co.veritrans.sdk.core.Logger;
import id.co.veritrans.sdk.core.SdkUtil;
import id.co.veritrans.sdk.core.StorageDataHandler;
import id.co.veritrans.sdk.core.VeritransSDK;
import id.co.veritrans.sdk.eventbus.bus.VeritransBusProvider;
import id.co.veritrans.sdk.eventbus.callback.GetCardBusCallback;
import id.co.veritrans.sdk.eventbus.callback.SaveCardBusCallback;
import id.co.veritrans.sdk.eventbus.callback.TokenBusCallback;
import id.co.veritrans.sdk.eventbus.callback.TransactionBusCallback;
import id.co.veritrans.sdk.eventbus.events.GeneralErrorEvent;
import id.co.veritrans.sdk.eventbus.events.GetCardFailedEvent;
import id.co.veritrans.sdk.eventbus.events.GetCardsSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.GetTokenFailedEvent;
import id.co.veritrans.sdk.eventbus.events.GetTokenSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.NetworkUnavailableEvent;
import id.co.veritrans.sdk.eventbus.events.SaveCardFailedEvent;
import id.co.veritrans.sdk.eventbus.events.SaveCardSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.TransactionFailedEvent;
import id.co.veritrans.sdk.eventbus.events.TransactionSuccessEvent;
import id.co.veritrans.sdk.eventbus.events.UpdateCreditCardDataFromScanEvent;
import id.co.veritrans.sdk.fragments.OffersListFragment;
import id.co.veritrans.sdk.fragments.PaymentTransactionStatusFragment;
import id.co.veritrans.sdk.models.BankDetail;
import id.co.veritrans.sdk.models.BankDetailArray;
import id.co.veritrans.sdk.models.BillingAddress;
import id.co.veritrans.sdk.models.CardPaymentDetails;
import id.co.veritrans.sdk.models.CardResponse;
import id.co.veritrans.sdk.models.CardTokenRequest;
import id.co.veritrans.sdk.models.CardTransfer;
import id.co.veritrans.sdk.models.CustomerDetails;
import id.co.veritrans.sdk.models.OffersListModel;
import id.co.veritrans.sdk.models.SaveCardRequest;
import id.co.veritrans.sdk.models.ScannerModel;
import id.co.veritrans.sdk.models.ShippingAddress;
import id.co.veritrans.sdk.models.TokenDetailsResponse;
import id.co.veritrans.sdk.models.TransactionDetails;
import id.co.veritrans.sdk.models.TransactionResponse;
import id.co.veritrans.sdk.models.UserAddress;
import id.co.veritrans.sdk.models.UserDetail;
import id.co.veritrans.sdk.utilities.Utils;
import id.co.veritrans.sdk.widgets.CirclePageIndicator;
import id.co.veritrans.sdk.widgets.MorphingButton;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by Ankit on 12/7/15.
 */
public class OffersActivity extends BaseActivity implements TransactionBusCallback, TokenBusCallback, SaveCardBusCallback, GetCardBusCallback {

    public static final int SCAN_REQUEST_CODE = 101;
    public static final String OFFERS_FRAGMENT = "offersList";
    public static final String ADD_CARD_FRAGMENT = "addCard";
    public static final String OFFER_POSITION = "offer_position";
    public static final String OFFER_NAME = "offer_name";
    public static final String OFFER_TYPE = "offer_type";
    public static final String OFFER_TYPE_BINPROMO = "offer_type_binpromo";
    public static final String OFFER_TYPE_INSTALMENTS = "offer_type_instalments";
    private static final int PAYMENT_WEB_INTENT = 200;
    private static final int PAY_USING_CARD = 61;
    public String currentFragment = "offersList";
    public ArrayList<OffersListModel> offersListModels = new ArrayList<>();
    public ArrayList<SaveCardRequest> creditCards = new ArrayList<>();
    private Toolbar toolbar = null;
    private TextView textViewTitleOffers = null;
    private VeritransSDK veritransSDK = null;
    private TextView textViewTitleCardDetails = null;
    private TextView textViewOfferName = null;
    private OffersListFragment offersListFragment = null;
    private int position = Constants.PAYMENT_METHOD_OFFERS;
    private OffersListModel selectedOffer;
    private int RESULT_CODE = RESULT_CANCELED;
    private String currentFragmentName;
    private FragmentManager fragmentManager;
    private float cardWidth;
    private UserDetail userDetail;
    private TokenDetailsResponse tokenDetailsResponse;
    private CardTokenRequest cardTokenRequest;
    private CardTransfer cardTransfer;
    private StorageDataHandler storageDataHandler;
    private RelativeLayout processingLayout;
    private ArrayList<BankDetail> bankDetails;
    private Subscription subscription;
    //for setResult
    private TransactionResponse transactionResponse = null;
    private String errorMessage = null;
    private CardPagerAdapter cardPagerAdapter;
    private CirclePageIndicator circlePageIndicator;
    private TextView emptyCardsTextView;
    private MorphingButton btnMorph;

    public OffersListModel getSelectedOffer() {
        return selectedOffer;
    }

    public void setSelectedOffer(OffersListModel selectedOffer) {
        this.selectedOffer = selectedOffer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offers);
        storageDataHandler = new StorageDataHandler();
        veritransSDK = VeritransSDK.getVeritransSDK();
        fragmentManager = getSupportFragmentManager();
        btnMorph = (MorphingButton) findViewById(R.id.btnMorph1);
        morphToCircle(0);

        // get position of selected payment method
        Intent data = getIntent();
        if (data != null) {
            position = data.getIntExtra(getString(R.string.position), Constants
                    .PAYMENT_METHOD_OFFERS);
        } else {
            SdkUtil.showSnackbar(OffersActivity.this, getString(R.string.error_something_wrong));
            finish();
        }

        initializeView();
        initializeTheme();
        if (!VeritransBusProvider.getInstance().isRegistered(this)) {
            VeritransBusProvider.getInstance().register(this);
        }
        OffersListFragment offersListFragment = new OffersListFragment();
        replaceFragment(offersListFragment, R.id.offers_container, true, false);
        calculateScreenWidth();
        // Only fetch credit card when card click type is two click or one click
        if (!veritransSDK.getTransactionRequest().getCardClickType().equals(getString(R.string.card_click_type_none))) {
            getCreditCards();
        }
        readBankDetails();
    }

    @Override
    protected void onDestroy() {
        if (VeritransBusProvider.getInstance().isRegistered(this)) {
            VeritransBusProvider.getInstance().unregister(this);
        }
        super.onDestroy();
    }

    private void initializeView() {
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        textViewTitleOffers = (TextView) findViewById(R.id.text_title);
        textViewTitleCardDetails = (TextView) findViewById(R.id.text_title_card_details);
        textViewOfferName = (TextView) findViewById(R.id.text_title_offer_name);
        //setup tool bar
        toolbar.setTitle(""); // disable default Text
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        processingLayout = (RelativeLayout) findViewById(R.id.processing_layout);

        textViewTitleCardDetails.setVisibility(View.GONE);
        textViewOfferName.setVisibility(View.GONE);
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
        SdkUtil.hideKeyboard(this);
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

    public void getToken(CardTokenRequest cardTokenRequest) {
        SdkUtil.showProgressDialog(this, getString(R.string.processing_payment), false);
        this.cardTokenRequest = cardTokenRequest;
        Logger.i("isSecure:" + this.cardTokenRequest.isSecure());
        veritransSDK.getToken(cardTokenRequest);
    }

    public void payUsingCard() {

        SdkUtil.showProgressDialog(this, getString(R.string.processing_payment), false);
        processingLayout.setVisibility(View.VISIBLE);
        //getSupportActionBar().setTitle(getString(R.string.processing_payment));
        textViewTitleOffers.setText(getString(R.string.processing_payment));
        CustomerDetails customerDetails = new CustomerDetails(userDetail.getUserFullName(), "",
                userDetail.getEmail(), userDetail.getPhoneNumber());

        ArrayList<UserAddress> userAddresses = userDetail.getUserAddresses();
        TransactionDetails transactionDetails = new TransactionDetails("" + veritransSDK.
                getTransactionRequest().getAmount(),
                veritransSDK.getTransactionRequest().getOrderId());
        if (userAddresses != null && !userAddresses.isEmpty()) {
            ArrayList<BillingAddress> billingAddresses = new ArrayList<>();
            ArrayList<ShippingAddress> shippingAddresses = new ArrayList<>();

            for (int i = 0; i < userAddresses.size(); i++) {
                UserAddress userAddress = userAddresses.get(i);
                if (userAddress.getAddressType() == Constants.ADDRESS_TYPE_BILLING ||
                        userAddress.getAddressType() == Constants.ADDRESS_TYPE_BOTH) {
                    BillingAddress billingAddress;
                    billingAddress = new BillingAddress();
                    billingAddress.setCity(userAddress.getCity());
                    billingAddress.setFirstName(userDetail.getUserFullName());
                    billingAddress.setLastName("");
                    billingAddress.setPhone(userDetail.getPhoneNumber());
                    billingAddress.setCountryCode(userAddress.getCountry());
                    billingAddress.setPostalCode(userAddress.getZipcode());
                    billingAddresses.add(billingAddress);
                }
                if (userAddress.getAddressType() == Constants.ADDRESS_TYPE_SHIPPING ||
                        userAddress.getAddressType() == Constants.ADDRESS_TYPE_BOTH) {
                    ShippingAddress shippingAddress;
                    shippingAddress = new ShippingAddress();
                    shippingAddress.setCity(userAddress.getCity());
                    shippingAddress.setFirstName(userDetail.getUserFullName());
                    shippingAddress.setLastName("");
                    shippingAddress.setPhone(userDetail.getPhoneNumber());
                    shippingAddress.setCountryCode(userAddress.getCountry());
                    shippingAddress.setPostalCode(userAddress.getZipcode());
                    shippingAddresses.add(shippingAddress);
                }

            }

            CardPaymentDetails cardPaymentDetails = null;
            try {
                Logger.i("savetokenid:" + cardTokenRequest.getSavedTokenId());
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            //for one click
            if (veritransSDK.getTransactionRequest().getCardClickType().equalsIgnoreCase
                    (getString(R.string.card_click_type_one_click)) &&
                    !TextUtils.isEmpty(cardTokenRequest.getSavedTokenId())) {
                if (cardTokenRequest.isInstalment()) {
                    cardPaymentDetails = new CardPaymentDetails(cardTokenRequest.getBank(),
                            cardTokenRequest.getSavedTokenId(), cardTokenRequest.isSaved(),
                            cardTokenRequest.getFormattedInstalmentTerm(), cardTokenRequest.getBins());
                } else {
                    cardPaymentDetails = new CardPaymentDetails(cardTokenRequest.getBank(),
                            cardTokenRequest.getSavedTokenId(), cardTokenRequest.isSaved());
                    cardPaymentDetails.setBinsArray(cardTokenRequest.getBins());
                }

            } else if (tokenDetailsResponse != null) {

                if (cardTokenRequest.isInstalment()) {
                    Logger.i("tokenDetailsResponse.getTokenId():" + tokenDetailsResponse.getTokenId());
                    cardPaymentDetails = new CardPaymentDetails(cardTokenRequest.getBank(),
                            tokenDetailsResponse.getTokenId(), cardTokenRequest.isSaved(),
                            cardTokenRequest.getFormattedInstalmentTerm(), cardTokenRequest.getBins());
                } else {
                    Logger.i("tokenDetailsResponse.getTokenId():" + tokenDetailsResponse.getTokenId());
                    cardPaymentDetails = new CardPaymentDetails(cardTokenRequest.getBank(),
                            tokenDetailsResponse.getTokenId(), cardTokenRequest.isSaved());
                    cardPaymentDetails.setBinsArray(cardTokenRequest.getBins());
                }


            } else {
                SdkUtil.hideProgressDialog();
                processingLayout.setVisibility(View.GONE);
                return;
            }
            cardTransfer = new CardTransfer(cardPaymentDetails, transactionDetails,
                    null, billingAddresses, shippingAddresses, customerDetails);
            //currentApiCallNumber = PAY_USING_CARD;

        }

        veritransSDK.paymentUsingCard(cardTransfer);

    }

    public VeritransSDK getVeritransSDK() {
        return veritransSDK;
    }

    public UserDetail getUserDetail() {
        return userDetail;
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


    public void saveCreditCards(SaveCardRequest creditCard) {
        /*try {
            storageDataHandler.writeObject(this, Constants.USERS_SAVED_CARD, creditCards);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        veritransSDK.saveCards(creditCard);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.i("reqCode:" + requestCode + ",res:" + resultCode);
        if (resultCode == RESULT_OK) {
            if (requestCode == PAYMENT_WEB_INTENT) {
                payUsingCard();
            } else if (requestCode == SCAN_REQUEST_CODE) {
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
      //  SdkUtil.showProgressDialog(this, getString(R.string.fetching_cards), true);
        textViewTitleOffers.setText(getString(R.string.fetching_cards));
        //  processingLayout.setVisibility(View.VISIBLE);
        veritransSDK.getSavedCard();
    }

    public void oneClickPayment(CardTokenRequest cardDetail) {
        this.cardTokenRequest = cardDetail;
        payUsingCard();
    }

    public void twoClickPayment(CardTokenRequest cardDetail) {
        SdkUtil.showProgressDialog(this, getString(R.string.processing_payment), false);
        this.cardTokenRequest = cardDetail;
        this.cardTokenRequest.setTwoClick(true);
        this.cardTokenRequest.setClientKey(veritransSDK.getClientKey());
        veritransSDK.getToken(cardTokenRequest);
    }

    public ArrayList<BankDetail> getBankDetails() {
        if (bankDetails != null && !bankDetails.isEmpty()) {
            return bankDetails;
        } else {
            return null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    private void readBankDetails() {

        if (bankDetails == null || bankDetails.isEmpty()) {
            rx.Observable<List<BankDetail>> banksObservable = rx.Observable.create(
                    new rx.Observable.OnSubscribe<List<BankDetail>>() {
                        @Override
                        public void call(Subscriber<? super List<BankDetail>> sub) {
                            try {
                                userDetail = LocalDataHandler.readObject(getString(R.string.user_details), UserDetail.class);
                                Logger.i("userDetail:" + userDetail.getUserFullName());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            ArrayList<BankDetail> bankDetails = new ArrayList<>();
                            try {
                                bankDetails = LocalDataHandler.readObject(getString(R.string.bank_details), BankDetailArray.class).getBankDetails();
                                Logger.i("bankDetails:" + bankDetails.size());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (bankDetails.isEmpty()) {
                                String json = null;
                                try {
                                    InputStream is = OffersActivity.this.getAssets().open("bank_details" +
                                            ".json");
                                    int size = is.available();
                                    byte[] buffer = new byte[size];
                                    is.read(buffer);
                                    is.close();
                                    json = new String(buffer, "UTF-8");
                                    Logger.i("json:" + json);
                                } catch (IOException ex) {
                                    ex.printStackTrace();

                                }

                                try {
                                    Gson gson = new Gson();
                                    bankDetails = gson.fromJson(json, BankDetailArray.class).getBankDetails();
                                    Logger.i("bankDetails:" + bankDetails.size());
                                    LocalDataHandler.saveObject(getString(R.string.bank_details), bankDetails);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                            sub.onNext(bankDetails);
                            sub.onCompleted();
                        }
                    }
            );
            Subscriber<List<BankDetail>> subscriber = new Subscriber<List<BankDetail>>() {

                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    Logger.i("error:" + e.getMessage());
                }

                @Override
                public void onNext(List<BankDetail> bankDetails) {
                    OffersActivity.this.bankDetails = (ArrayList<BankDetail>) bankDetails;
                    Logger.i("bankdetail getter onnext" + bankDetails.size());
                }
            };
            subscription = banksObservable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(subscriber);

        }
    }

    public void setResultAndFinish() {
        Intent data = new Intent();
        data.putExtra(getString(R.string.transaction_response), transactionResponse);
        data.putExtra(getString(R.string.error_transaction), errorMessage);
        setResult(RESULT_CODE, data);
        finish();
    }

    public void setResultCode(int resultCode) {
        this.RESULT_CODE = resultCode;
    }

    public void setAdapterViews(CardPagerAdapter cardPagerAdapter, CirclePageIndicator circlePageIndicator, TextView emptyCardsTextView) {
        this.cardPagerAdapter = cardPagerAdapter;
        this.circlePageIndicator = circlePageIndicator;
        this.emptyCardsTextView = emptyCardsTextView;
    }

    public void morphingAnimation(){
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
    public MorphingButton.Params morphCicle(int time) {
        return MorphingButton.Params.create()
                .cornerRadius(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                .width(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                .height(Utils.dpToPx(Constants.FAB_HEIGHT_DP))
                .duration(time)
                .colorPressed(color(R.color.colorAccent))
                .color(color(R.color.colorAccent));


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

    public MorphingButton getBtnMorph() {
        return btnMorph;
    }
    
    public void morphToCircle(int time) {
        MorphingButton.Params circle = morphCicle(time);
        btnMorph.morph(circle);
    }

    @Subscribe
    @Override
    public void onEvent(GetTokenSuccessEvent event) {
        TokenDetailsResponse tokenDetailsResponse = event.getResponse();
        if (tokenDetailsResponse != null) {
            cardTokenRequest.setBank(tokenDetailsResponse.getBank());
        }
        this.tokenDetailsResponse = tokenDetailsResponse;
        if (tokenDetailsResponse != null) {
            Logger.i("token suc:" + tokenDetailsResponse.getTokenId() + ","
                    + veritransSDK.getTransactionRequest().isSecureCard());
            if (veritransSDK.getTransactionRequest().isSecureCard()) {
                SdkUtil.hideProgressDialog();
                if (tokenDetailsResponse.getRedirectUrl() != null &&
                        !tokenDetailsResponse.getRedirectUrl().equals("")) {
                    Intent intentPaymentWeb = new Intent(this, PaymentWebActivity.class);
                    intentPaymentWeb.putExtra(Constants.WEBURL, tokenDetailsResponse.getRedirectUrl());
                    startActivityForResult(intentPaymentWeb, PAYMENT_WEB_INTENT);
                }
            } else {
                SdkUtil.showProgressDialog(this, getString(R.string.processing_payment), false);
                payUsingCard();
            }
        }

    }

    @Subscribe
    @Override
    public void onEvent(GetTokenFailedEvent event) {
        SdkUtil.hideProgressDialog();
        SdkUtil.showApiFailedMessage(this, event.getMessage());
    }

    @Subscribe
    @Override
    public void onEvent(TransactionSuccessEvent event) {
        TransactionResponse cardPaymentResponse = event.getResponse();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processingLayout.setVisibility(View.GONE);
            }
        }, 200);

        SdkUtil.hideProgressDialog();
        Logger.i("cardPaymentResponse:" + cardPaymentResponse.getStatusCode());

        if (cardPaymentResponse.getStatusCode().equalsIgnoreCase(getString(R.string.success_code_200)) ||
                cardPaymentResponse.getStatusCode().equalsIgnoreCase(getString(R.string.success_code_201))) {

            transactionResponse = cardPaymentResponse;

            PaymentTransactionStatusFragment paymentTransactionStatusFragment =
                    PaymentTransactionStatusFragment.newInstance(cardPaymentResponse);
            replaceFragment(paymentTransactionStatusFragment, R.id.offers_container,  true, false);
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            //getSupportActionBar().setTitle(getString(R.string.title_payment_successful));
            textViewTitleOffers.setText(getString(R.string.title_payment_status));
            if (cardTokenRequest.isSaved()) {
                if (!creditCards.isEmpty()) {
                    int position = -1;
                    for (int i = 0; i < creditCards.size(); i++) {
                        SaveCardRequest card = creditCards.get(i);
                        if (card.getSavedTokenId().equalsIgnoreCase(cardTokenRequest.getSavedTokenId())) {
                            position = i;
                            break;
                        }
                    }
                    if (position >= 0) {
                        creditCards.remove(position);
                    }
                }
                cardTokenRequest.setCardCVV("0");
                cardTokenRequest.setClientKey("");
                cardTokenRequest.setGrossAmount(0);
                //to save card number in xxxxxxxxxxxx1118 format
                /*String formatedCardNumber = cardTokenRequest.getFormatedCardNumber();
                formatedCardNumber = formatedCardNumber.replace("-","");
                cardTokenRequest.setCardNumber(formatedCardNumber);*/
                //cardTokenRequest.setBank(Constants.BANK_NAME);
                if (bankDetails != null && !bankDetails.isEmpty()) {
                    String firstSix = cardTokenRequest.getCardNumber().substring(0, 6);
                    for (BankDetail bankDetail : bankDetails) {
                        if (bankDetail.getBin().equalsIgnoreCase(firstSix)) {
                            cardTokenRequest.setBank(bankDetail.getIssuing_bank());
                            cardTokenRequest.setCardType(bankDetail.getCard_association());
                            break;
                        }
                    }
                }

                if (cardTokenRequest.isSaved() && !TextUtils.isEmpty(cardPaymentResponse
                        .getSavedTokenId())) {
                    cardTokenRequest.setSavedTokenId(cardPaymentResponse.getSavedTokenId());
                }
                Logger.i("Card:" + cardTokenRequest.getString());

                SaveCardRequest saveCardRequest = new SaveCardRequest();
                saveCardRequest.setSavedTokenId(cardTokenRequest.getSavedTokenId());
                saveCreditCards(saveCardRequest);
                creditCards.add(saveCardRequest);
            }
        }
    }

    @Subscribe
    @Override
    public void onEvent(TransactionFailedEvent event) {
        TransactionResponse transactionResponse = event.getResponse();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processingLayout.setVisibility(View.GONE);
            }
        }, 200);
        OffersActivity.this.transactionResponse = transactionResponse;
        OffersActivity.this.errorMessage = event.getMessage();
        SdkUtil.hideProgressDialog();
        PaymentTransactionStatusFragment paymentTransactionStatusFragment =
                PaymentTransactionStatusFragment.newInstance(transactionResponse);
        replaceFragment(paymentTransactionStatusFragment, R.id.offers_container, true, false);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        //getSupportActionBar().setTitle(getString(R.string.title_payment_failed));
        textViewTitleOffers.setText(getString(R.string.title_payment_status));
    }

    @Subscribe
    @Override
    public void onEvent(NetworkUnavailableEvent event) {

    }

    @Subscribe
    @Override
    public void onEvent(GeneralErrorEvent event) {

    }

    @Subscribe
    @Override
    public void onEvent(GetCardsSuccessEvent event) {
        CardResponse cardResponse = event.getResponse();
        //  SdkUtil.hideProgressDialog();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                processingLayout.setVisibility(View.GONE);
            }
        }, 200);

        Logger.i("cards api successful" + cardResponse);
        if (cardResponse != null && !cardResponse.getData().isEmpty()) {

            creditCards.clear();
            creditCards.addAll(cardResponse.getData());
            if (cardPagerAdapter != null && circlePageIndicator != null) {
                cardPagerAdapter.notifyDataSetChanged();
                circlePageIndicator.notifyDataSetChanged();
            }
            //processingLayout.setVisibility(View.GONE);
            if (emptyCardsTextView != null) {
                if (!creditCards.isEmpty()) {
                    emptyCardsTextView.setVisibility(View.GONE);
                } else {
                    emptyCardsTextView.setVisibility(View.VISIBLE);
                }

            }

        }
    }

    @Subscribe
    @Override
    public void onEvent(GetCardFailedEvent event) {
        SdkUtil.hideProgressDialog();
        Logger.i("card fetching failed :" + event.getMessage());
        processingLayout.setVisibility(View.GONE);
    }

    @Subscribe
    @Override
    public void onEvent(SaveCardSuccessEvent event) {
        Logger.i("card saved");
    }

    @Subscribe
    @Override
    public void onEvent(SaveCardFailedEvent event) {

    }
}

