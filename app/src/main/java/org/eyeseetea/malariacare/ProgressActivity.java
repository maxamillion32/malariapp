/*
 * Copyright (c) 2015.
 *
 * This file is part of QA App.
 *
 *  Health Network QIS App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Health Network QIS App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.eyeseetea.malariacare.database.iomodules.dhis.exporter.PushController;
import org.eyeseetea.malariacare.database.iomodules.dhis.importer.PullController;
import org.eyeseetea.malariacare.database.iomodules.dhis.importer.SyncProgressStatus;
import org.eyeseetea.malariacare.database.model.Survey;
import org.eyeseetea.malariacare.database.utils.Session;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;

public class ProgressActivity extends Activity {

    private static final String TAG=".ProgressActivity";
    public static final String IS_A_PUSH = "EXTRA_IS_A_PUSH";
    private static final int MAX_PULL_STEPS=7;
    private static final int MAX_PUSH_STEPS=4;

    ProgressBar progressBar;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);
        prepareUI();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.pull_metadata), false);
        editor.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        Dhis2Application.bus.register(this);
        launchAction();
    }

    @Override
    public void onPause() {
        super.onPause();
        Dhis2Application.bus.unregister(this);
    }

    private void prepareUI(){
        progressBar=(ProgressBar)findViewById(R.id.pull_progress);
        progressBar.setMax(isAPush()?MAX_PUSH_STEPS:MAX_PULL_STEPS);
        textView=(TextView)findViewById(R.id.pull_text);
    }

    @Subscribe
    public void onProgressChange(final SyncProgressStatus syncProgressStatus) {
        if(syncProgressStatus ==null){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (syncProgressStatus.hasError()) {
                    showStatus(syncProgressStatus.getException().getMessage());
                    return;
                }

                //Step
                if (syncProgressStatus.hasProgress()) {
                    step(syncProgressStatus.getMessage());
                    return;
                }

                //Finish
                if (syncProgressStatus.isFinish()) {
                    showAndGoDashboard();
                }
            }
        });
    }

    /**
     * Launches a pull or push according to an intent extra
     */
    private void launchAction(){
        //Push or Pull according to extra param from intent
        if(isAPush()){
            Survey survey= Session.getSurvey();
            PushController.getInstance().push(this,survey);
        }else{
            PullController.getInstance().pull(this);
        }
    }

    /**
     * Shows a dialog with the given message
     * @param msg
     */
    private void showStatus(String msg){
        boolean isAPush=isAPush();
        String title=getDialogTitle(isAPush);

        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton(android.R.string.yes, null).create().show();
    }

    /**
     * Prints the step in the progress bar
     * @param msg
     */
    private void step(final String msg) {
        final int currentProgress = progressBar.getProgress();
        progressBar.setProgress(currentProgress + 1);
        textView.setText(msg);
    }

    /**
     * Shows a dialog to tell that pull is done and then moves into the dashboard
     */
    private void showAndGoDashboard() {
        boolean isAPush=isAPush();
        String title=getDialogTitle(isAPush);
        int msg=isAPush?R.string.dialog_push_success:R.string.dialog_pull_success;

        step(getString(R.string.progress_pull_done));

        //XXX If pull move this to a function
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.pull_metadata), true);
        editor.commit();

        new AlertDialog.Builder(this)
				.setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setNeutralButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        Intent i = new Intent(ProgressActivity.this, DashboardActivity.class);
                        startActivity(i);
                        finish();
                    }
                }).create().show();
    }

    private boolean isAPush() {
        Intent i=getIntent();
        return (i!=null && i.getBooleanExtra(IS_A_PUSH,false));
    }

    private String getDialogTitle(boolean isAPush){
        int stringId=isAPush?R.string.dialog_title_push_response:R.string.dialog_title_pull_response;
        return getString(stringId);
    }


}
