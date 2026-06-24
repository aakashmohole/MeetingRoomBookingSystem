package com.meetingroom.util;

import org.zkoss.zk.ui.util.Clients;

public class ToastUtil {
    public static void success(String message) {
        show(message, "success");
    }

    public static void error(String message) {
        show(message, "error");
    }

    public static void warning(String message) {
        show(message, "warning");
    }

    public static void info(String message) {
        show(message, "info");
    }

    private static void show(String message, String type) {
        if (message == null) return;
        String escaped = message.replace("\\", "\\\\")
                                .replace("'", "\\'")
                                .replace("\n", " ")
                                .replace("\r", " ");
        
        String js = "if (typeof window.showToast === 'undefined') {\n" +
                "    window.createToastContainer = function() {\n" +
                "        var container = document.getElementById('toast-container');\n" +
                "        if (!container) {\n" +
                "            container = document.createElement('div');\n" +
                "            container.id = 'toast-container';\n" +
                "            container.style.position = 'fixed';\n" +
                "            container.style.top = '24px';\n" +
                "            container.style.right = '24px';\n" +
                "            container.style.zIndex = '99999';\n" +
                "            container.style.display = 'flex';\n" +
                "            container.style.flexDirection = 'column';\n" +
                "            container.style.gap = '12px';\n" +
                "            container.style.pointerEvents = 'none';\n" +
                "            document.body.appendChild(container);\n" +
                "        }\n" +
                "        return container;\n" +
                "    };\n" +
                "    window.showToast = function(msg, t) {\n" +
                "        var container = window.createToastContainer();\n" +
                "        var toast = document.createElement('div');\n" +
                "        toast.style.background = t === 'success' ? '#10b981' : t === 'error' ? '#ef4444' : t === 'warning' ? '#f59e0b' : '#3b82f6';\n" +
                "        toast.style.color = '#ffffff';\n" +
                "        toast.style.padding = '14px 24px';\n" +
                "        toast.style.borderRadius = '12px';\n" +
                "        toast.style.boxShadow = '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)';\n" +
                "        toast.style.fontFamily = \"'Inter', -apple-system, BlinkMacSystemFont, sans-serif\";\n" +
                "        toast.style.fontSize = '0.9rem';\n" +
                "        toast.style.fontWeight = '600';\n" +
                "        toast.style.opacity = '0';\n" +
                "        toast.style.transform = 'translateY(-20px) scale(0.95)';\n" +
                "        toast.style.transition = 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)';\n" +
                "        toast.style.pointerEvents = 'auto';\n" +
                "        toast.style.display = 'flex';\n" +
                "        toast.style.alignItems = 'center';\n" +
                "        toast.style.gap = '12px';\n" +
                "        toast.style.border = '1px solid rgba(255,255,255,0.1)';\n" +
                "        \n" +
                "        var icon = document.createElement('span');\n" +
                "        icon.style.display = 'flex';\n" +
                "        icon.style.alignItems = 'center';\n" +
                "        icon.style.justifyContent = 'center';\n" +
                "        icon.style.width = '20px';\n" +
                "        icon.style.height = '20px';\n" +
                "        icon.style.borderRadius = '50%';\n" +
                "        icon.style.background = 'rgba(255, 255, 255, 0.2)';\n" +
                "        icon.style.fontSize = '0.85rem';\n" +
                "        icon.innerHTML = t === 'success' ? '✓' : t === 'error' ? '✕' : t === 'warning' ? '⚠' : 'ℹ';\n" +
                "        toast.appendChild(icon);\n" +
                "        \n" +
                "        var text = document.createElement('span');\n" +
                "        text.innerText = msg;\n" +
                "        toast.appendChild(text);\n" +
                "        \n" +
                "        container.appendChild(toast);\n" +
                "        \n" +
                "        setTimeout(function() {\n" +
                "            toast.style.opacity = '1';\n" +
                "            toast.style.transform = 'translateY(0) scale(1)';\n" +
                "        }, 10);\n" +
                "        \n" +
                "        setTimeout(function() {\n" +
                "            toast.style.opacity = '0';\n" +
                "            toast.style.transform = 'translateY(-20px) scale(0.95)';\n" +
                "            setTimeout(function() { toast.remove(); }, 300);\n" +
                "        }, 4000);\n" +
                "    };\n" +
                "}\n" +
                "window.showToast('" + escaped + "', '" + type + "');";
        
        Clients.evalJavaScript(js);
    }
}
