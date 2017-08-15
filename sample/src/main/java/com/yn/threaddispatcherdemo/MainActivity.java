package com.yn.threaddispatcherdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.yn.DispatcherIndex;
import com.yn.dispatcher.Switcher;
import com.yn.threaddispatcher4android.UiDispatcher;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Whyn on 2017/8/10.
 */

public class MainActivity extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //configure defautl switcher
        Switcher.builder()
                .setUiExecutor(new UiDispatcher())
                .setIndex(new DispatcherIndex())
                .installDefaultThreadDispatcher();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Switcher.getDefault().shutdown();
    }

    @OnClick(R.id.btnNormalUsage)
    public void onNormalUsage() {
        startActivity(new Intent(this, NormalUsageActivity.class));
    }

    @OnClick(R.id.btnInterfaceUsage)
    public void onInterfaceMethodUsage() {
        startActivity(new Intent(this, InterfaceMethodUsageActivity.class));
    }

    @OnClick(R.id.btnAliasUsage)
    public void onAliasUsage() {
        startActivity(new Intent(this, MethodAliasUsageActivity.class));
    }
}
