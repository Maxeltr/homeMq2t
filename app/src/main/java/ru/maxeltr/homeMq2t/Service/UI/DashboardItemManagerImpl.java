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
import ru.maxeltr.homeMq2t.Config.UIPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.CardModel;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.Status;

public class DashboardItemManagerImpl implements DashboardItemManager {

    private static final Logger logger = LoggerFactory.getLogger(DashboardItemManagerImpl.class);

    @Autowired
    @Qualifier("getUIPropertiesProvider")
    private UIPropertiesProvider appProperties;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    private final ObjectMapper mapper;

    public DashboardItemManagerImpl() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Msg.Builder getCardSettings(Msg.Builder msg) {
        msg.type(MediaType.APPLICATION_JSON_VALUE);

        Optional<CardModel> cardSettingsOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            cardSettingsOpt = this.appProperties.getCardSettings(msg.getId());
        } else {
            cardSettingsOpt = this.appProperties.getEmptyCardSettings();
            logger.debug("New card was created. Card={}", cardSettingsOpt.get());
        }

        if (cardSettingsOpt.isPresent()) {
            logger.info("Settings retrieved successfully. Card={}", msg.getId());
            msg.data(this.jsonFormatter.createJson(cardSettingsOpt.get().getHtml(), "onEditCardSettings", Status.OK));
        } else {
            logger.warn("Could not get settings for card={}.", msg.getId());
            msg.data(this.jsonFormatter.createJson("", "onEditCardSettings", Status.FAIL));
        }

        msg.timestamp(String.valueOf(Instant.now().toEpochMilli()));

        return msg;
    }

    @Override
    public void saveCardSettings(Msg.Builder msg) {
        CardEntity cardEntity;
        JsonNode root;

        try {
            root = mapper.readTree(msg.getData());
            DashboardEntity dashboardEntity = appProperties.getDashboardEntity(root.path("dashboardNumber").asText()).orElseThrow();
            cardEntity = mapper.readValue(msg.getData(), CardEntity.class);
            cardEntity.setDashboard(dashboardEntity);
            var entity = this.appProperties.saveCardEntity(cardEntity);
            logger.debug("Saved card settings {}.", entity);
        } catch (JsonProcessingException | NoSuchElementException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex.getMessage());

        }
    }

}
