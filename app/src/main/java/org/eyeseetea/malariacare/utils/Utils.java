/*
 * Copyright (c) 2015.
 *
 * This file is part of Facility QA Tool App.
 *
 *  Facility QA Tool App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Facility QA Tool App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eyeseetea.malariacare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.raizlabs.android.dbflow.structure.BaseModel;

import org.eyeseetea.malariacare.database.model.CompositeScore;
import org.eyeseetea.malariacare.database.model.Header;
import org.eyeseetea.malariacare.database.model.Question;
import org.eyeseetea.malariacare.database.model.Tab;
import org.eyeseetea.malariacare.database.utils.PreferencesState;
import org.eyeseetea.malariacare.database.utils.Session;
import org.eyeseetea.malariacare.layout.utils.QuestionRow;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class Utils {

    static final int numberOfDecimals = 0; // Number of decimals outputs will have

    public static String round(float base, int decimalPlace){
        BigDecimal bd = new BigDecimal(Float.toString(base));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        if (decimalPlace == 0) return Integer.toString((int) bd.floatValue());
        return Float.toString(bd.floatValue());
    }

    public static String round(float base){
        return round(base, Utils.numberOfDecimals);
    }

    public static List<BaseModel> convertTabToArrayCustom(Tab tab) {
        List<BaseModel> result = new ArrayList<BaseModel>();

        for (Header header : tab.getHeaders()) {
            result.add(header);
            for (Question question : header.getQuestions()) {
                if (tab.getType().equals(Constants.TAB_AUTOMATIC) || tab.getType().equals(Constants.TAB_AUTOMATIC_NON_SCORED) || question.hasChildren())
                    result.add(question);
            }
        }

        return result;
    }

    public static List preloadTabItems(Tab tab, String module){
        List<? extends BaseModel> items = Session.getTabsCache().get(tab.getId_tab());

        if (tab.isCompositeScore())
            items = CompositeScore.listByProgram(Session.getSurveyByModule(module).getProgram(),null);

        else{

            items=Session.getTabsCache().get(tab.getId_tab());

            if (items == null) {
                items = convertTabToArrayCustom(tab);
            }
            Session.getTabsCache().put(tab.getId_tab(), items);
        }

        return compressTabItems(items);
    }

    /**
     * Turns a list of headers, questions into a list of headers, questions and questionRows.
     * @param items
     * @return
     */
    public static List compressTabItems(List items){
        List<Object> compressedItems = new ArrayList<>();
        Iterator<Object> iterator = items.iterator();
        QuestionRow lastRow=null;
        while(iterator.hasNext()){
            Object item = iterator.next();

            //Header
            if(item instanceof Header){
                compressedItems.add(item);
                continue;
            }

            //Normal question
            if(item instanceof Question && !((Question)item).belongsToCustomTab()){
                compressedItems.add(item);
                continue;
            }

            //Custom tabs questions/titles
            Question question = (Question) item;
            //Question that belongs to a customtab
            if(question.isCustomTabNewRow()){
                lastRow = new QuestionRow();
                compressedItems.add(lastRow);
            }
            lastRow.addQuestion(question);
        }
        return compressedItems;
    }

    public static StringBuilder convertFromInputStreamToString(InputStream inputStream){
        StringBuilder stringBuilder = new StringBuilder();

        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuilder;
    }


    public static String formatDate(Date date){
        if(date==null){
            return "-";
        }
        Locale locale = PreferencesState.getInstance().getContext().getResources().getConfiguration().locale;
        DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
        return dateFormatter.format(date);
    }


    /**
     * This method check if the Internet conexion is active
     * @return return true if all is correct.
     */
    public static boolean isNetworkAvailable(){
        ConnectivityManager cm =
                (ConnectivityManager) PreferencesState.getInstance().getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if(netInfo==null)
            return false;
        return netInfo.isConnected();
    }
}
