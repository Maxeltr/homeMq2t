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

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import ru.maxeltr.homeMq2t.Config.CommandPropertiesProvider;
import ru.maxeltr.homeMq2t.Entity.CommandEntity;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.Msg;
import ru.maxeltr.homeMq2t.Model.MsgImpl;
import ru.maxeltr.homeMq2t.Model.ViewModel;

public class CommandManagerImpl implements DashboardItemManager {

    @Autowired
    @Qualifier("getCommandPropertiesProvider")
    private CommandPropertiesProvider propertiesProvider;

    @Autowired
    private UIJsonFormatter jsonFormatter;

    @Override
    public Msg getItem(Msg msg) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Msg getItemsByDashboard(Msg msg) {
        Optional<ViewModel> dashboardOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            dashboardOpt = this.propertiesProvider.getDashboard(msg.getId());
        } else {
            dashboardOpt = this.propertiesProvider.getCommandDashboard();
        }

        return MsgImpl.newBuilder()
                .data(this.jsonFormatter.createJson(dashboardOpt, "onDisplayCommandList"))
                .type(MediaType.APPLICATION_JSON_VALUE)
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    @Override
    public Msg getItemSettings(Msg msg) {
        Optional<ViewModel> commandSettingsOpt;
        if (StringUtils.isNotBlank(msg.getId())) {
            commandSettingsOpt = this.propertiesProvider.getCommandSettings(msg.getId());
        } else {
            commandSettingsOpt = this.propertiesProvider.getEmptyCommandSettings();
            //logger.debug("New command was created. Command={}", commandSettingsOpt.orElse(""));
        }

        return msg.toBuilder()
                .type(MediaType.APPLICATION_JSON_VALUE)
                .data(this.jsonFormatter.createJson(commandSettingsOpt, "onEditCommandSettings"))
                .timestamp(String.valueOf(Instant.now().toEpochMilli()))
                .build();
    }

    @Override
    public void saveItemSettings(Msg msg) {
        CommandEntity commandEntity;
        JsonNode root;

        try {
            root = mapper.readTree(msg.getData());
            commandEntity = mapper.readValue(msg.getData(), CommandEntity.class);
            var entity = this.propertiesProvider.saveCommandEntity(commandEntity);
            logger.debug("Saved command settings {}.", entity);
        } catch (JsonProcessingException | NoSuchElementException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex.getMessage());
        }
    }

    @Override
    public void deleteItem(Msg msg) {
        JsonNode root;
        try {
            root = mapper.readTree(msg.getData());
            String id = root.path(CommandEntity.JSON_FIELD_ID).asText();
            this.propertiesProvider.deleteCommand(id);
            logger.debug("Deletetd command {}.", msg);
        } catch (JsonProcessingException | NoSuchElementException ex) {
            logger.warn("Could not convert json data={} to map. {}", msg, ex.getMessage());

        }
    }

}
