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

package org.eyeseetea.malariacare.database.iomodules.dhis.importer;

import android.util.Log;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.eyeseetea.malariacare.database.iomodules.dhis.importer.models.DataElementExtended;
import org.eyeseetea.malariacare.database.iomodules.dhis.importer.models.OptionExtended;
import org.eyeseetea.malariacare.database.model.CompositeScore;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.Option;
import org.hisp.dhis.android.sdk.persistence.models.ProgramStageDataElement;
import org.hisp.dhis.android.sdk.persistence.models.ProgramStageDataElement$Table;
import org.hisp.dhis.android.sdk.persistence.models.meta.DbOperation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by arrizabalaga on 13/11/15.
 */
public class CompositeScoreBuilder {

    private static final String TAG=".CompositeScoreBuilder";

    /**
     * Expected value for the attributeValue DeQuesType in those dataElements which are a CompositeScore
     */
    private static final String COMPOSITE_SCORE_NAME ="COMPOSITE_SCORE";

    /**
     * Hierarquical code for any root compositeScore
     */
    public static final String ROOT_NODE_CODE = "0";

    /**
     * Holds every compositeScore to calculate its order and parent according to its programStage and hierarchicalCode
     * programstageId -> hierarchical code -> Score
     */
    static Map<String,Map<String,CompositeScore>> mapCompositeScores;

    CompositeScoreBuilder(){
        mapCompositeScores=new HashMap<>();

        Option optionCompositeScore= OptionExtended.findOptionByName(COMPOSITE_SCORE_NAME);
        //No Option with COMPOSITE_SCORE -> error
        if(optionCompositeScore==null){
            Log.e(TAG,"There is no option named 'COMPOSITE_SCORE' which is a severe data error");
        }
    }

    /**
     * Registers a compositeScore in builder
     */
    public void add(CompositeScore compositeScore){

        //Find the right 'tabgroup' to group scores by program
        String programStageUID=DataElementExtended.findProgramStageByDataElementUID(compositeScore.getUid());
        if(programStageUID==null){
            Log.e(TAG,String.format("Cannot find tabgroup for composite score: %s",compositeScore.getLabel()));
            return;
        }

        //Get the map of scores for that program&
        Map<String,CompositeScore> compositeScoresXProgram=mapCompositeScores.get(programStageUID);
        if(compositeScoresXProgram==null){
            compositeScoresXProgram = new HashMap<String, CompositeScore>();
            mapCompositeScores.put(programStageUID, compositeScoresXProgram);
        }

        //Annotate the composite score in its proper map (x tabgroup|program)
        compositeScoresXProgram.put(compositeScore.getHierarchical_code(), compositeScore);
    }

    /**
     * Builds scores fulfilling its order and parent attributes.
     * This operation requires that every composite score has already been registered and procesed and thus It cant be done during 'visit'
     */
    public void buildScores(){
        for(Map.Entry<String,Map<String,CompositeScore>> mapXProgramStage: mapCompositeScores.entrySet()){
            buildOrder(mapXProgramStage.getValue());
            buildHierarchy(mapXProgramStage.getValue());
        }
    }

    /**
     * Completes the orderBy attribute in compositeScore according to its hierarchical code
     */
    private List<DbOperation> buildOrder(Map<String,CompositeScore> compositeScoreMap){

        List<DbOperation> operations = new ArrayList<>();
        //Order scores by its hierarchical code
        List<CompositeScore> scores = new ArrayList(compositeScoreMap.values());
        Collections.sort(scores,new CompositeScoreComparator());

        int i=0;
        for(CompositeScore score:scores){
            score.setOrder_pos(Integer.valueOf(i));
            operations.add(DbOperation.save(score));
            i++;
        }
        return operations;
    }

    /**
     * Registers a compositeScore in builder
     */
    private List<DbOperation> buildHierarchy(Map<String,CompositeScore> compositeScoreMap){
        List<DbOperation> operations = new ArrayList<>();
        CompositeScore rootScore = compositeScoreMap.get(ROOT_NODE_CODE);

        //Find the parent of each score
        for(CompositeScore compositeScore:compositeScoreMap.values()){

            //Hierarchical code
            String compositeScoreHierarchicalCode=compositeScore.getHierarchical_code();

            //Root node has no parent
            if(ROOT_NODE_CODE.equals(compositeScore.getHierarchical_code())){
                continue;
            }

            //Count number of dots
            int numDots = compositeScoreHierarchicalCode.length() - compositeScoreHierarchicalCode.replace(".", "").length();

            //0 dots -> parent: root | X dots -> substring minus last index
            String parentHierarchicalCode = (numDots==0)?ROOT_NODE_CODE:compositeScoreHierarchicalCode.substring(0,compositeScoreHierarchicalCode.length()-2);

            compositeScore.setCompositeScore(compositeScoreMap.get(parentHierarchicalCode));
            operations.add(DbOperation.save(compositeScore));
        }
        return operations;
    }

    public static CompositeScore getCompositeScoreFromDataElementAndHierarchicalCode(DataElement dataElement, String HierarchicalCode){
        CompositeScore compositeScore=null;
        String programId= DataElementExtended.findProgramStageByDataElementUID(dataElement.getUid());
        Map<String,CompositeScore> compositeScoresInProgram=mapCompositeScores.get(programId);
        compositeScore=compositeScoresInProgram.get(HierarchicalCode);
        return compositeScore;
    }

    /**
     * Comparator that order composite score by its hierarchical code
     */
    class CompositeScoreComparator implements Comparator<CompositeScore>{

        @Override
        public int compare(CompositeScore lhs, CompositeScore rhs) {
            return lhs.getHierarchical_code().compareTo(rhs.getHierarchical_code());
        }

        @Override
        public boolean equals(Object object) {
            return false;
        }
    }
}
