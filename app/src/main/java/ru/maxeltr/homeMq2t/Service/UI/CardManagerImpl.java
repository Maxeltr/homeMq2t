/*
 * The MIT License
 *
 * Copyright 2025 Maxim Eltratov <<Maxim.Eltratov@ya.ru>>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ru.maxeltr.homeMq2t.Service.UI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Model.Msg;

public class CardManagerImpl implements DashboardItemManager {

    private static final Logger logger = LoggerFactory.getLogger(CardManagerImpl.class);

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider cardProperties;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    private final ObjectMapper mapper;

    public CardManagerImpl() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Msg getItemsByDashboard(Msg msg) {
        Optional<ViewModel> dashboardOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            dashboardOpt = this.cardProperties.getDashboard(msg.getId());
        } else {
            dashboardOpt = this.cardProperties.getStartDashboard();
        }

        return msg.toBuilder()
                .data(this.jsonFormatter.createJson(dashboardOpt, "onDisplayDashboard"))
                .type(MediaType.APPLICATION_JSON_VALUE)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    @Override
    public Msg getItemSettings(Msg msg) {
        Optional<ViewModel> cardSettingsOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            cardSettingsOpt = this.cardProperties.getCardSettings(msg.getId());
        } else {
            cardSettingsOpt = this.cardProperties.getEmptyCardSettings();
            //logger.debug("New card was created. Card={}", cardSettingsOpt.orElse(""));
        }

        return msg.toBuilder()
                .data(this.jsonFormatter.createJson(cardSettingsOpt, "onEditCardSettings"))
                .type(MediaType.APPLICATION_JSON_VALUE)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    @Override
    public void saveItemSettings(Msg msg) {
        CardEntity cardEntity;
        JsonNode root;

        try {
            root = mapper.readTree(msg.getData());
            DashboardEntity dashboardEntity = cardProperties.getDashboardEntity(root.path("dashboardNumber").asText()).orElseThrow();
            cardEntity = mapper.readValue(msg.getData(), CardEntity.class);
            cardEntity.setDashboard(dashboardEntity);
            var entity = this.cardProperties.saveCardEntity(cardEntity);
            logger.debug("Saved card settings {}.", entity);
        } catch (JsonProcessingException | NoSuchElementException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex.getMessage());

        }
    }

    @Override
    public void deleteItem(Msg msg) {
        JsonNode root;

        try {
            root = mapper.readTree(msg.getData());
            String id = root.path("ID").asText();
            this.cardProperties.deleteCard(id);
            logger.debug("Deletetd card {}.", msg);
        } catch (JsonProcessingException | NoSuchElementException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex.getMessage());

        }
    }

    @Override
    public Msg getItem(Msg msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void saveItem(Msg msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
