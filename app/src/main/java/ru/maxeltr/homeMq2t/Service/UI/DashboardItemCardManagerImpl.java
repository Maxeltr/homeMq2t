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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.maxeltr.homeMq2t.Config.CardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.DashboardPropertiesProvider;
import ru.maxeltr.homeMq2t.Config.MediaTypes;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.Status;

public class DashboardItemCardManagerImpl implements DashboardItemManager {

    private static final Logger logger = LoggerFactory.getLogger(DashboardItemCardManagerImpl.class);

    private final Lock lock = new ReentrantLock();

    @Autowired
    @Qualifier("getCardPropertiesProvider")
    private CardPropertiesProvider propertiesProvider;

    @Autowired
    @Qualifier("getDashboardPropertiesProvider")
    private DashboardPropertiesProvider dashboardPropertiesProvider;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    private final ObjectMapper mapper;

    public DashboardItemCardManagerImpl() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Retrieve items for a dashboard and return a message whose data contains
     * the JSON representation of the dashboard view model. If msg.getId() is
     * not blank, the dashboard with that id(number) is requested from property
     * provider. Otherwise the configured start dashboard is used.
     *
     * @param msg incoming message containing optional dashboard number
     * @return a new Msg built from the incoming with JSON payload, type set to
     * APPLICATION/JSON and an updated timestamp. Json payload contains the
     * event name, status, content type and Base64-encoded HTML with optional
     * error or unknown status prefix
     */
    @Override
    public Msg getItemsByDashboard(Msg msg) {
        lock.lock();
        try {
            Optional<ViewModel<DashboardEntity>> dashboardOpt;
            if (StringUtils.isNotBlank(msg.getId())) {
                dashboardOpt = this.dashboardPropertiesProvider.getCardDashboard(msg.getId());
            } else {
                dashboardOpt = this.dashboardPropertiesProvider.getStartDashboard();
            }

            return msg.toBuilder()
                    .data(dashboardOpt
                            .map(viewModel -> jsonFormatter.createAndEncodeHtml(viewModel.getHtml(), Status.OK))
                            .orElseGet(() -> jsonFormatter.createAndEncodeHtml("", Status.FAIL))
                    )
                    .type(MediaTypes.TEXT_HTML_BASE64.getValue())
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrive settings for single card and return a Msg whose data contains the
     * JSON representation of the card settings view model.
     *
     * If msg.getId() is not blank, settings for that card id are requested
     * otherwise an empty/new card settings model is returned.
     *
     * @param msg incoming message containing optional card id
     * @return a new Msg built from incoming message with JSON payload, type set
     * to APPLICATION/JSON and an updated timestamp. Json payload contains the
     * event name, status, content type and Base64-encoded HTML with optional
     * error or unknown status prefix
     */
    @Override
    public Msg getItemSettings(Msg msg) {
        lock.lock();
        try {
            Optional<ViewModel> cardSettingsOpt;
            if (StringUtils.isNotBlank(msg.getId())) {
                cardSettingsOpt = this.propertiesProvider.getCardSettings(msg.getId());
            } else {
                cardSettingsOpt = this.propertiesProvider.getEmptyCardSettings();
            }

            return msg.toBuilder()
                    .data(cardSettingsOpt
                            .map(viewModel -> jsonFormatter.createAndEncodeHtml(viewModel.getHtml(), Status.OK))
                            .orElseGet(() -> jsonFormatter.createAndEncodeHtml("", Status.FAIL))
                    )
                    .type(MediaTypes.TEXT_HTML_BASE64.getValue())
                    .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                    .build();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Parse JSON payload from msg, resolve the referenced dashboard entity, map
     * the payload to CardEntity, associate it with the resolved DashboardEntity
     * and persist the card entity via property provider.
     *
     * @param msg incoming message whose data contains JSON representing
     * CardEntity.
     */
    @Override
    public void saveItemSettings(Msg msg) {
        lock.lock();
        try {
            JsonNode root = mapper.readTree(msg.getData());
            DashboardEntity dashboardEntity = dashboardPropertiesProvider.getDashboardEntity(root.path("dashboardNumber").asText()).orElseThrow();
            CardEntity cardEntity = mapper.readValue(msg.getData(), CardEntity.class);
            cardEntity.setDashboard(dashboardEntity);
            var entity = this.propertiesProvider.saveCardEntity(cardEntity);
            logger.debug("Saved card settings {}.", entity);
        } catch (JsonProcessingException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex);
        } catch (NoSuchElementException ex) {
            logger.warn("Could not find entity for id={}. {}", msg.getId(), ex);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Parse the JSON payload from Msg and delete the referenced card.
     *
     * @param msg incoming message whose data contains a JSON with an ID field.
     */
    @Override
    public void deleteItem(Msg msg) {
        lock.lock();
        try {
            JsonNode root = mapper.readTree(msg.getData());
            String id = root.path(CardEntity.JSON_FIELD_ID).asText();
            this.propertiesProvider.deleteCard(id);
            logger.debug("Deleted card {}.", msg);
        } catch (JsonProcessingException ex) {
            logger.warn("Could not delete data={}. {}", msg, ex);

        } finally {
            lock.unlock();
        }
    }

    @Override
    public Msg getItem(Msg msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
