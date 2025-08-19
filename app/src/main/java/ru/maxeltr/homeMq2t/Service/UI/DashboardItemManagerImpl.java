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
import ru.maxeltr.homeMq2t.Config.CommandPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.UIPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.Status;

public class DashboardItemManagerImpl implements DashboardItemManager {

    private static final Logger logger = LoggerFactory.getLogger(DashboardItemManagerImpl.class);

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider cardProperties;

    @Autowired
    @Qualifier("getCommandPropertiesProvider")
    private CommandPropertiesProvider commandProperties;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    private final ObjectMapper mapper;

    public DashboardItemManagerImpl() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public Msg getDashboard(Msg msg) {
        Optional<ViewModel> dashboardOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            dashboardOpt = this.cardProperties.getDashboard(msg.getId());
        } else {
            dashboardOpt = this.cardProperties.getStartDashboard();
        }

        return msg.toBuilder()
                .data(createJson(dashboardOpt, "onDisplayDashboard"))
                .type(MediaType.APPLICATION_JSON_VALUE)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    @Override
    public Msg getCardSettings(Msg msg) {
        Optional<ViewModel> cardSettingsOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            cardSettingsOpt = this.cardProperties.getCardSettings(msg.getId());
        } else {
            cardSettingsOpt = this.cardProperties.getEmptyCardSettings();
            logger.debug("New card was created. Card={}", cardSettingsOpt.get());
        }

        return msg.toBuilder()
                .data(createJson(cardSettingsOpt, "onEditCardSettings"))
                .type(MediaType.APPLICATION_JSON_VALUE)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    @Override
    public Msg getCommands() {
        Optional<ViewModel> commandSettingsOpt = this.commandProperties.getCommands();

    }

    @Override
    public Msg getCommandSettings(Msg msg) {
        Optional<ViewModel> commanddSettingsOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            commanddSettingsOpt = this.commandProperties.getCommandSettings(msg.getId());
        } else {
            commanddSettingsOpt = this.commandProperties.getEmptyCommandSettings();
            logger.debug("New command was created. Command={}", commanddSettingsOpt.get());
        }

        return msg.toBuilder()
                .type(MediaType.APPLICATION_JSON_VALUE)
                .data(createJson(commanddSettingsOpt, "onEditCommandSettings"))
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    private String createJson(Optional<ViewModel> modelOpt, String name) {
        if (modelOpt.isPresent()) {
            return this.jsonFormatter.createJson(modelOpt.get().getHtml(), name, Status.OK);
        }

        return this.jsonFormatter.createJson("", name, Status.FAIL);
    }

    @Override
    public void saveCardSettings(Msg msg) {
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
    public void deleteCard(Msg msg) {
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

}
