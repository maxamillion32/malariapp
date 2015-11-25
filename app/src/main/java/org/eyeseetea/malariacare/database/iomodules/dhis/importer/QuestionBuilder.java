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

import org.eyeseetea.malariacare.R;
import org.eyeseetea.malariacare.database.iomodules.dhis.importer.models.DataElementExtended;
import org.eyeseetea.malariacare.database.model.CompositeScore;
import org.eyeseetea.malariacare.database.model.Header;;
import org.eyeseetea.malariacare.database.model.Match;
import org.eyeseetea.malariacare.database.model.Option;
import org.eyeseetea.malariacare.database.model.Question;
import org.eyeseetea.malariacare.database.model.QuestionOption;
import org.eyeseetea.malariacare.database.model.QuestionRelation;
import org.eyeseetea.malariacare.database.model.Tab;
import org.eyeseetea.malariacare.database.utils.PreferencesState;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.meta.DbOperation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ignac on 14/11/2015.
 */
public class QuestionBuilder {

    private static final String TAG = ".QuestionBuilder";

    Map<String, Question> questions;

    Map<String, String> questionParents;

    Map<String, String> questionRols;

    Map<String, String> questionLevels;

    Map<Long, Match> matches;
    /**
     * Mapping all the Match question parents
     */
    Map<String, String> matchTypes;
    /**
     * Mapping all the Match question type(child/parent)
     */
    Map<String, String> matchLevels;

    Map<String, String> matchParents;

    Map<String, List<String>> matchChildren;

    Map<String, Header> mapHeader;

    Map<Long, QuestionRelation> questionRelations;

    Map<Long, QuestionOption> questionOptions;

    /**
     * It is needed in the header order.
     */
    private int header_order = 0;

    QuestionBuilder() {
        questions = new HashMap<>();
        mapHeader = new HashMap<>();
        questionRols = new HashMap<>();
        questionLevels = new HashMap<>();
        questionParents = new HashMap<>();
        matchLevels = new HashMap<>();
        matchParents = new HashMap<>();
        matchChildren = new HashMap<>();
        matchTypes = new HashMap<>();
        questionRelations = new HashMap<>();
        matches = new HashMap<>();
        questionOptions = new HashMap<>();
    }

    /**
     * Registers a Question in builder
     *
     * @param question
     */
    public void add(Question question) {
        questions.put(question.getUid(), question);
    }

    /**
     * Save and return Header question
     * <p/>
     * The header check if exist before be saved.
     *
     * @param dataElementExtended
     * @return header question
     */
    public Header findHeader(DataElementExtended dataElementExtended) {
        Header header = null;
        String value = dataElementExtended.getValue(DataElementExtended.ATTRIBUTE_HEADER_NAME);
        if (value != null) {
            if (!mapHeader.containsKey(value)) {
                header = new Header();
                header.setName(value.trim());
                header.setShort_name(value);
                value = dataElementExtended.getValue(DataElementExtended.ATTRIBUTE_TAB_NAME);
                Tab questionTab = new Tab();
                questionTab = (Tab) ConvertFromSDKVisitor.appMapObjects.get(questionTab.getClass() + value);
                header.setOrder_pos(header_order);
                header_order++;
                header.setTab(questionTab);
                //header.save();
                mapHeader.put(header.getName(), header);
            } else
                header = mapHeader.get(value);
        }
        return header;
    }

    /**
     * Registers a Parent/child and Match Parent/child relations in maps
     * Its need the programid to diferenciate between programs dataelements.
     *
     * @param dataElementExtended
     */
    public void RegisterParentChildRelations(DataElementExtended dataElementExtended) {
        DataElement dataElement = dataElementExtended.getDataElement();
        String questionRelationType = null;
        String questionRelationGroup = null;
        String matchRelationType = null;
        String matchRelationGroup = null;
        questionRelationType = dataElementExtended.getValue(DataElementExtended.ATTRIBUTE_HIDE_TYPE);
        questionRelationGroup = dataElementExtended.getValue(DataElementExtended.ATTRIBUTE_HIDE_GROUP);
        matchRelationType = dataElementExtended.getValue(DataElementExtended.ATTRIBUTE_MATCH_TYPE);
        matchRelationGroup = dataElementExtended.getValue(DataElementExtended.ATTRIBUTE_MATCH_GROUP);
        if (questionRelationType != null) {
            String parentProgramUid = DataElementExtended.findProgramUIDByDataElementUID(dataElement.getUid());
            if (questionRelationType.equals(DataElementExtended.PARENT)) {
                questionParents.put(parentProgramUid + questionRelationGroup, dataElement.getUid());
            } else {
                questionRols.put(parentProgramUid + dataElement.getUid(), questionRelationType);
                questionLevels.put(parentProgramUid + dataElement.getUid(), questionRelationGroup);
            }
        }
        if (matchRelationType != null) {
            String parentProgramUid = DataElementExtended.findProgramUIDByDataElementUID(dataElement.getUid());
            if (matchRelationType.equals(DataElementExtended.PARENT)) {
                matchParents.put(parentProgramUid + matchRelationGroup, dataElement.getUid());
            } else if (matchRelationType.equals(DataElementExtended.CHILD)) {
                List<String> childsUids;
                if (matchChildren.containsKey(parentProgramUid + matchRelationGroup)) {
                    childsUids = matchChildren.get(parentProgramUid + matchRelationGroup);
                } else {
                    childsUids = new ArrayList<>();
                }
                childsUids.add(dataElement.getUid());
                matchChildren.put(parentProgramUid + matchRelationGroup, childsUids);
            }
            matchTypes.put(parentProgramUid + dataElement.getUid(), matchRelationType);
            matchLevels.put(parentProgramUid + dataElement.getUid(), matchRelationGroup);
        }

    }

    /**
     * Save Question id_parent QuestionOption QuestionRelation and Match
     *
     * @param dataElementExtended
     */
    public List<DbOperation> addRelations(DataElementExtended dataElementExtended) {
        List<DbOperation> operations = new ArrayList<>();
        if (questions.containsKey(dataElementExtended.getDataElement().getUid())) {
            operations.addAll(addParent(dataElementExtended.getDataElement()));
            operations.addAll(addQuestionRelations(dataElementExtended.getDataElement()));
            operations.addAll(addCompositeScores(dataElementExtended));
        }
        return operations;
    }

    private List<DbOperation> addCompositeScores(DataElementExtended dataElementExtended) {
        List<DbOperation> operations = new ArrayList<>();
        CompositeScore compositeScore = dataElementExtended.findCompositeScore();
        if (compositeScore != null) {
            Question appQuestion = questions.get(dataElementExtended.getDataElement().getUid());
            if (appQuestion != null) {
                appQuestion.setCompositeScore(compositeScore);
                operations.add(DbOperation.save(appQuestion));
                questions.put(dataElementExtended.getDataElement().getUid(), appQuestion);
                add(appQuestion);
            }
        }
        return operations;
    }


    /**
     * Save Question id_parent in Question
     *
     * @param dataElement
     */
    private List<DbOperation> addParent(DataElement dataElement) {
        List<DbOperation> operations = new ArrayList<>();
        String programUid = DataElementExtended.findProgramUIDByDataElementUID(dataElement.getUid());
        String questionRelationType = questionRols.get(programUid + dataElement.getUid());
        String questionRelationGroup = questionLevels.get(programUid + dataElement.getUid());

        Question appQuestion = questions.get(dataElement.getUid());

        if (questionRelationType != null && questionRelationType.equals(DataElementExtended.CHILD)) {
            if (questionRelationType.equals(DataElementExtended.CHILD)) {
                String parentuid = questionParents.get(programUid + questionRelationGroup);
                if (parentuid != null) {
                    QuestionRelation questionRelation = new QuestionRelation();
                    questionRelation.setOperation(QuestionRelation.PARENT_CHILD);
                    questionRelation.setQuestion(appQuestion);
                    boolean isSaved=false;
                    Question parentQuestion = questions.get(parentuid);
                    List<Option> options = parentQuestion.getAnswer().getOptions();
                    for (Option option : options) {
                        if (option.getName().equals(PreferencesState.getInstance().getContext().getResources().getString(R.string.yes))) {
                            if(!isSaved) {
                                //the questionRelation only created if have child with yes option
                                operations.add(DbOperation.save(questionRelation));
                                questionRelations.put(questionRelation.getId_question_relation(), questionRelation);
                                isSaved=true;
                            }
                            Match match = new Match();
                            match.setQuestionRelation(questionRelation);
                            operations.add(DbOperation.save(match));
                            matches.put(match.getId_match(), match);
                            QuestionOption questionOption = new QuestionOption(option, parentQuestion, match);
                            operations.add(DbOperation.save(questionOption));
                            questionOptions.put(questionOption.getId_question_option(), questionOption);
                        }
                    }
                }
            }
        }
        return operations;
    }

    /**
     * Create QuestionOption QuestionRelation and Match relations
     * <p/>
     * checks if the dataElement is a parent(if is a parent it have matchTypes and matchLevels)
     * Later get the two childs and create the relation
     * it needs check what Options factors do match, and check it with method getMatchOption() .
     *
     * @param dataElement
     */
    private List<DbOperation> addQuestionRelations(DataElement dataElement) {

        List<DbOperation> operations = new ArrayList<>();
        String programUid = DataElementExtended.findProgramUIDByDataElementUID(dataElement.getUid());
        String matchRelationType = matchTypes.get(programUid + dataElement.getUid());
        String matchRelationGroup = matchLevels.get(programUid + dataElement.getUid());
        Question appQuestion = questions.get(dataElement.getUid());

        if (matchRelationType != null && matchRelationType.equals(DataElementExtended.PARENT)) {
            List<String> mapChilds = matchChildren.get(programUid + matchRelationGroup);
            List<Question> children = new ArrayList<>();
            children.add(questions.get(mapChilds.get(0)));
            children.add(questions.get(mapChilds.get(1)));

            if (questions.get(mapChilds.get(0)) != null && questions.get(mapChilds.get(1)) != null) {
                QuestionRelation questionRelation = new QuestionRelation();
                questionRelation.setOperation(0);
                questionRelation.setQuestion(appQuestion);
                operations.add(DbOperation.save(questionRelation));
                questionRelations.put(questionRelation.getId_question_relation(), questionRelation);
                operations.addAll(questionRelation.createMatchFromQuestions(children));
            }
        }
        return operations;
    }
}
