package tn.cofat.sense;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.drawerlayout.widget.DrawerLayout;

public final class NavigationHelper {
    private NavigationHelper() {}

    public static void setup(Activity activity) {
        // Navigation items
        bind(activity, "navProduction", ProductionActivity.class);
        bind(activity, "navErrors", ErrorsActivity.class);
        bind(activity, "navPrevention", PreventionActivity.class);
        bind(activity, "navInterventions", InterventionsActivity.class);
        bind(activity, "navHealth", HealthActivity.class);
        bind(activity, "navCopilot", CopilotActivity.class);

        // Bottom menu items (often used in some layouts)
        bind(activity, "menuProduction", ProductionActivity.class);
        bind(activity, "menuErrors", ErrorsActivity.class);
        bind(activity, "menuPrevention", PreventionActivity.class);
        bind(activity, "menuInterventions", InterventionsActivity.class);
        bind(activity, "menuHealth", HealthActivity.class);
        bind(activity, "menuCopilot", CopilotActivity.class);

        // Logout items
        bindLogout(activity, "tvLogout");
        bindLogout(activity, "menuLogout");
        bindLogout(activity, "logoutButton");

        // Drawer toggle
        setupDrawer(activity);
    }

    private static void setupDrawer(Activity activity) {
        int btnId = activity.getResources().getIdentifier("btnMenu", "id", activity.getPackageName());
        int drawerId = activity.getResources().getIdentifier("drawerLayout", "id", activity.getPackageName());
        int menuId = activity.getResources().getIdentifier("drawerMenu", "id", activity.getPackageName());

        if (btnId != 0 && drawerId != 0 && menuId != 0) {
            View btnMenu = activity.findViewById(btnId);
            DrawerLayout drawerLayout = activity.findViewById(drawerId);
            View drawerMenu = activity.findViewById(menuId);

            if (btnMenu != null && drawerLayout != null && drawerMenu != null) {
                btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(drawerMenu));
            }
        }
    }

    private static void bind(Activity activity, String idName, Class<? extends Activity> target) {
        int id = activity.getResources().getIdentifier(idName, "id", activity.getPackageName());
        if (id == 0) return;
        View view = activity.findViewById(id);
        if (view == null) return;
        view.setOnClickListener(v -> {
            if (activity.getClass().equals(target)) {
                // Close drawer if already on this activity
                closeDrawer(activity);
                return;
            }
            activity.startActivity(new Intent(activity, target));
            activity.finish();
        });
    }

    private static void bindLogout(Activity activity, String idName) {
        int id = activity.getResources().getIdentifier(idName, "id", activity.getPackageName());
        if (id == 0) return;
        View view = activity.findViewById(id);
        if (view == null) return;
        view.setOnClickListener(v -> logout(activity));
    }

    private static void closeDrawer(Activity activity) {
        int drawerId = activity.getResources().getIdentifier("drawerLayout", "id", activity.getPackageName());
        if (drawerId != 0) {
            DrawerLayout drawer = activity.findViewById(drawerId);
            if (drawer != null) {
                drawer.closeDrawers();
            }
        }
    }

    public static void logout(Activity activity) {
        activity.getSharedPreferences("COFAT_SESSION", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
