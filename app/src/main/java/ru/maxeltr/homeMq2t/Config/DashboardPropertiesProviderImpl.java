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
package ru.maxeltr.homeMq2t.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.CommandEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.CardImpl;
import ru.maxeltr.homeMq2t.Model.CommandImpl;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.DashboardImpl;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Repository.CommandRepository;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

public class DashboardPropertiesProviderImpl implements DashboardPropertiesProvider {

    private static final Logger logger = LoggerFactory.getLogger(DashboardPropertiesProviderImpl.class);

    @Autowired
    private Environment env;

    public final static String DASHBOARD_TEMPLATE_PATH = "dashboard-template-path";

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private CardRepository cardRepository;

    @Override
    public Optional<ViewModel<DashboardEntity>> getCommandDashboard() {
        String commandPathname = env.getProperty(AppProperties.COMMAND_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(commandPathname)) {
            logger.warn("No value defined for command template pathname.");
            return Optional.empty();
        }

        String dashboardPathname = env.getProperty(DASHBOARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(dashboardPathname)) {
            logger.warn("No value defined for dashboard template pathname.");
            return Optional.empty();
        }

        Optional<DashboardEntity> dashboardEntityOpt = dashboardRepository.findByName(AppProperties.COMMAND_LIST_NAME);
        if (dashboardEntityOpt.isEmpty()) {
            return Optional.empty();
        }

        List<ViewModel> commands = new ArrayList<>();
        List<CommandEntity> commandEntities = commandRepository.findAll();
        commandEntities.forEach(commandEntity -> {
            ViewModel command = new CommandImpl(commandEntity, commandPathname);
            commands.add(command);
            logger.debug("Command={} has been created and added to command list. Number={}", command.getName(), command.getNumber());
        });
        logger.debug("Create command list with size={}.", commands.size());

        return Optional.of(new DashboardImpl(dashboardEntityOpt.get(), commands, dashboardPathname));
    }

    @Override
    public Optional<DashboardEntity> getDashboardEntity(String number) {
        return AppUtils.safeParseInt(number).flatMap(dashboardRepository::findByNumber);
    }

    @Override
    public Optional<ViewModel<DashboardEntity>> getDashboard(String number) {
        String dashboardPathname = env.getProperty(DASHBOARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(dashboardPathname)) {
            logger.warn("No value defined for dashboard template pathname.");
            return Optional.empty();
        }

        String cardPathname = env.getProperty(AppProperties.CARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(cardPathname)) {
            logger.warn("No value defined for card template pathname.");
            return Optional.empty();
        }

        Optional<DashboardEntity> dashboardEntityOpt = AppUtils.safeParseInt(number).flatMap(dashboardRepository::findByNumber);
        if (dashboardEntityOpt.isEmpty()) {
            logger.warn("No dashboard found with number={}", number);
            return Optional.empty();
        }
        DashboardEntity dashboardEntity = dashboardEntityOpt.get();

        List<ViewModel> cards = new ArrayList<>();
        List<CardEntity> cardEntities = cardRepository.findByDashboardNumber(dashboardEntity.getNumber());
        cardEntities.forEach(cardEntity -> {
            ViewModel card = new CardImpl(cardEntity, cardPathname);
            cards.add(card);
            logger.debug("Card={} has been created and added to card list. Number={}", card.getName(), card.getNumber());
        });
        logger.debug("Create card list with size={}.", cards.size());
        ViewModel<DashboardEntity> dashboard = new DashboardImpl(dashboardEntity, cards, dashboardPathname);
        logger.debug("Dashboard={} has been created.", dashboard.getName());

        return Optional.of(dashboard);

    }

    @Override
    public Optional<ViewModel<DashboardEntity>> getStartDashboard() {
        return this.getAllDashboards().stream().findFirst();
    }

    /**
     * Creates a list of dashboards based on the configuration properties
     * defined in the database. Each dashboard can contain multiple cards, which
     * are defined in the configuration.
     *
     * @return a list of dashboards created from configuration. If no dashboards
     * are defined, an empty list is returned.
     */
    @Override
    public List<ViewModel<DashboardEntity>> getAllDashboards() {
        List<ViewModel<DashboardEntity>> dashboards = new ArrayList<>();

        String dashboardPathname = env.getProperty(DASHBOARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(dashboardPathname)) {
            logger.info("No value defined for dashboard template pathname.");
            return dashboards;
        }

        String cardPathname = env.getProperty(AppProperties.CARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(cardPathname)) {
            logger.info("No value defined for card template pathname.");
            return dashboards;
        }

        List<DashboardEntity> dashboardEntities = dashboardRepository.findAll();
        dashboardEntities.forEach(dashboardEntity -> {
            List<ViewModel> cards = new ArrayList<>();
            List<CardEntity> cardEntities = cardRepository.findByDashboardNumber(dashboardEntity.getNumber());
            cardEntities.forEach(cardEntity -> {
                ViewModel card = new CardImpl(cardEntity, cardPathname);
                cards.add(card);
                logger.info("Card={} has been created and added to card list. Number={}", card.getName(), card.getNumber());
            });
            logger.info("Create card list with size={}.", cards.size());
            ViewModel<DashboardEntity> dashboard = new DashboardImpl(dashboardEntity, cards, dashboardPathname);
            dashboards.add(dashboard);
            logger.info("Dashboard={} has been created and added to dashboard list.", dashboard.getName());
        });

        logger.info("Create dashbord list with size={}.", dashboards.size());

        return dashboards;
    }

}
