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
import ru.maxeltr.homeMq2t.Entity.BaseEntity;
import ru.maxeltr.homeMq2t.Entity.CardEntity;
import ru.maxeltr.homeMq2t.Entity.CommandEntity;
import ru.maxeltr.homeMq2t.Entity.ComponentEntity;
import ru.maxeltr.homeMq2t.Entity.DashboardEntity;
import ru.maxeltr.homeMq2t.Model.CardImpl;
import ru.maxeltr.homeMq2t.Model.CommandImpl;
import ru.maxeltr.homeMq2t.Model.ComponentImpl;
import ru.maxeltr.homeMq2t.Model.Dashboard;
import ru.maxeltr.homeMq2t.Model.DashboardImpl;
import ru.maxeltr.homeMq2t.Model.ViewModel;
import ru.maxeltr.homeMq2t.Repository.CardRepository;
import ru.maxeltr.homeMq2t.Repository.CommandRepository;
import ru.maxeltr.homeMq2t.Repository.ComponentRepository;
import ru.maxeltr.homeMq2t.Repository.DashboardRepository;
import ru.maxeltr.homeMq2t.Utils.AppUtils;

public class DashboardPropertiesProviderImpl implements DashboardPropertiesProvider {

    private static final Logger logger = LoggerFactory.getLogger(DashboardPropertiesProviderImpl.class);

    @Autowired
    private Environment env;

    public final static String DASHBOARD_TEMPLATE_PATH = "dashboard-template-path";

    @Autowired
    private DashboardRepository<CardEntity> dashboardRepository;

    @Autowired
    private CommandRepository commandRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Override
    public Optional<ViewModel<DashboardEntity>> getCommandDashboard() {
        String commandPathname = env.getProperty(CommandPropertiesProvider.COMMAND_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(commandPathname)) {
            logger.warn("No value defined for command template pathname.");
            return Optional.empty();
        }

        List<ViewModel<CommandEntity>> commands = new ArrayList<>();
        List<CommandEntity> commandEntities = commandRepository.findAll();
        commandEntities.forEach(commandEntity -> {
            ViewModel<CommandEntity> command = new CommandImpl(commandEntity, commandPathname);
            commands.add(command);
            logger.debug("Command={} has been created and added to command list. Number={}", command.getName(), command.getNumber());
        });
        logger.debug("Create command list with size={}.", commands.size());

        return getDashboardByName(commands, CommandPropertiesProvider.COMMAND_LIST_NAME);
    }

    @Override
    public Optional<ViewModel<DashboardEntity>> getComponentDashboard() {
        String componentPathname = env.getProperty(ComponentPropertiesProvider.COMPONENT_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(componentPathname)) {
            logger.warn("No value defined for component template pathname.");
            return Optional.empty();
        }

        List<ViewModel<ComponentEntity>> components = new ArrayList<>();
        List<ComponentEntity> componentEntities = componentRepository.findAll();
        componentEntities.forEach(ComponentEntity -> {
            ViewModel<ComponentEntity> component = new ComponentImpl(ComponentEntity, componentPathname);
            components.add(component);
            logger.debug("Component={} has been created and added to component list. Number={}", component.getName(), component.getNumber());
        });
        logger.debug("Create component list with size={}.", components.size());

        return getDashboardByName(components, ComponentPropertiesProvider.COMPONENT_LIST_NAME);
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

        Optional<DashboardEntity<CardEntity>> dashboardEntityOpt = AppUtils.safeParseInt(number).flatMap(dashboardRepository::findByNumber);
        if (dashboardEntityOpt.isEmpty()) {
            logger.warn("No dashboard found with number={}", number);
            return Optional.empty();
        }
        DashboardEntity<CardEntity> dashboardEntity = dashboardEntityOpt.get();

        List<ViewModel<CardEntity>> cards = getCardsFromDashboardEntity(dashboardEntity);
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
            logger.warn("No value defined for dashboard template pathname.");
            return dashboards;
        }

        List<DashboardEntity<CardEntity>> dashboardEntities = dashboardRepository.findAll();
        dashboardEntities.forEach(dashboardEntity -> {
            List<ViewModel<CardEntity>> cards = getCardsFromDashboardEntity(dashboardEntity);
            ViewModel<DashboardEntity> dashboard = new DashboardImpl(dashboardEntity, cards, dashboardPathname);
            dashboards.add(dashboard);
            logger.debug("Dashboard={} has been created and added to dashboard list.", dashboard.getName());
        });

        logger.debug("Create dashbord list with size={}.", dashboards.size());

        return dashboards;
    }

    private List<ViewModel<CardEntity>> getCardsFromDashboardEntity(DashboardEntity<CardEntity> dashboardEntity) {
        List<ViewModel<CardEntity>> cards = new ArrayList<>();
        String cardPathname = env.getProperty(CardPropertiesProvider.CARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(cardPathname)) {
            logger.warn("No value defined for card template pathname.");
            return cards;
        }

        List<CardEntity> cardEntities = dashboardEntity.getCards();
        cardEntities.forEach(cardEntity -> {
            ViewModel<CardEntity> card = new CardImpl(cardEntity, cardPathname);
            cards.add(card);
            logger.debug("Card={} has been created and added to card list. Number={}", card.getName(), card.getNumber());
        });
        logger.debug("Create card list with size={}.", cards.size());

        return cards;
    }

    private <T extends BaseEntity> Optional<ViewModel<DashboardEntity>> getDashboardByName(List<ViewModel<T>> entities, String dashboardName) {
        String dashboardPathname = env.getProperty(DASHBOARD_TEMPLATE_PATH, "");
        if (StringUtils.isEmpty(dashboardPathname)) {
            logger.warn("No value defined for dashboard template pathname.");
            return Optional.empty();
        }

        Optional<DashboardEntity> dashboardEntityOpt = dashboardRepository.findByName(dashboardName);
        if (dashboardEntityOpt.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new DashboardImpl(dashboardEntityOpt.get(), entities, dashboardPathname));
    }
}
