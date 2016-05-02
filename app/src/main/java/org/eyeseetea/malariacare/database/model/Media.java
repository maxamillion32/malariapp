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

package org.eyeseetea.malariacare.database.model;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.eyeseetea.malariacare.database.AppDatabase;

import java.util.List;

@Table(databaseName = AppDatabase.NAME)
public class Media extends BaseModel{

     // Values to identify image/video on media_type column in the DB
    public static final int MEDIA_TYPE_IMAGE = 0;

    public static final int MEDIA_TYPE_VIDEO = 1;

    @Column
    @PrimaryKey(autoincrement = true)
    long id_media;

    @Column
    int media_type;

    @Column
    String resource_url;

    @Column
    Long id_question;

    /**
     * Reference to the question
     */
    Question question;

    public Media(){}

    public Media(int media_type, String resource_url, Question question){
        this.media_type = media_type;
        this.resource_url = resource_url;
        this.setQuestion(question);
    }

    public Question getQuestion(){
        if(question==null){
            if(id_question==null) return null;
            question = new Select()
                    .from(Question.class)
                    .where(Condition.column(Question$Table.ID_QUESTION)
                        .is(id_question)).querySingle();
        }
        return question;
    }

    public int getMediaType() {
        return media_type;
    }

    public void setMediaType(int media_type) {
        this.media_type = media_type;
    }

    public String getResourceUrl() {
        return resource_url;
    }

    public void setResourceUrl(String resource_url) {
        this.resource_url = resource_url;
    }

    public static List<Media> getAllMedia() {
        return new Select().all().from(Media.class).queryList();
    }

    public void setQuestion(Question question){
        this.question = question;
        this.id_question = (question!=null)?question.getId_question():null;
    }

    public void setQuestion(Long id_question){
        this.id_question = id_question;
        this.question = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Media media = (Media) o;

        if (id_media != media.id_media) return false;
        if (getMediaType() != media.getMediaType()) return false;
        if (getResourceUrl() != null ? !getResourceUrl().equals(media.getResourceUrl()) : media.getResourceUrl() != null)
            return false;
        return id_question.equals(media.id_question);

    }

    @Override
    public int hashCode() {
        int result = (int) (id_media ^ (id_media >>> 32));
        result = 31 * result + getMediaType();
        result = 31 * result + (getResourceUrl() != null ? getResourceUrl().hashCode() : 0);
        result = 31 * result + id_question.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Media{" +
                "id_media=" + id_media +
                ", media_type=" + media_type +
                ", resource_url='" + resource_url + '\'' +
                ", id_question=" + id_question +
                '}';
    }
}
