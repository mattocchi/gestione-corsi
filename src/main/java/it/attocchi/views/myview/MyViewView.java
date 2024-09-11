package it.attocchi.views.myview;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import it.attocchi.views.MainLayout;

@PageTitle("Pre Iscrizione")
@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class MyViewView extends Composite<VerticalLayout> {

    public MyViewView() {
//        HorizontalLayout layoutRow2 = new HorizontalLayout();
//        HorizontalLayout layoutRow = new HorizontalLayout();
//        VerticalLayout layoutColumn2 = new VerticalLayout();
//        VerticalLayout layoutColumn3 = new VerticalLayout();
//        HorizontalLayout layoutRow3 = new HorizontalLayout();
//        getContent().setWidth("100%");
//        getContent().getStyle().set("flex-grow", "1");
//        layoutRow2.addClassName(Gap.MEDIUM);
//        layoutRow2.setWidth("100%");
//        layoutRow2.setHeight("min-content");
//        layoutRow.addClassName(Gap.MEDIUM);
//        layoutRow.setWidth("100%");
//        layoutRow.getStyle().set("flex-grow", "1");
//        layoutColumn2.getStyle().set("flex-grow", "1");
//        layoutColumn3.setWidth("100%");
//        layoutColumn3.getStyle().set("flex-grow", "1");
//        layoutRow3.addClassName(Gap.MEDIUM);
//        layoutRow3.setWidth("100%");
//        layoutRow3.setHeight("min-content");
//        getContent().add(layoutRow2);
//        getContent().add(layoutRow);
//        layoutRow.add(layoutColumn2);
//        layoutRow.add(layoutColumn3);
//        getContent().add(layoutRow3);

        TextField firstName = new TextField("First name");
        TextField lastName = new TextField("Last name");
        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        PasswordField confirmPassword = new PasswordField("Confirm password");

        FormLayout formLayout = new FormLayout();
        formLayout.add(firstName, lastName, username, password,
                confirmPassword);
        formLayout.setResponsiveSteps(
                // Use one column by default
                new FormLayout.ResponsiveStep("0", 1),
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("500px", 2));
// Stretch the username field over 2 columns
        formLayout.setColspan(username, 2);

        getContent().add(formLayout);
    }
}
