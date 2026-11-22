/* =========================================
   YUMMYPREP - SHARED SITE SCRIPT
   Shows the logged-in customer in the navbar on every page.
   Other scripts can use:  YummyAuth.getUser(function (user) { ... })
   ========================================= */

(function () {

    var scriptEl = document.currentScript;
    var base = scriptEl ? scriptEl.src : window.location.href;

    var API_URL = new URL("../php/auth_api.php", base).href;
    var PROFILE_URL = new URL("../pages/profile.html", base).href;
    var LOGIN_URL = new URL("../pages/login.html", base).href;

    var KEYS = ["yummyPrepLoggedIn", "yummyPrepCurrentUser", "yummyPrepUser"];

    /* ---------- storage helpers ---------- */

    function readStoredUser() {
        try {
            var raw = sessionStorage.getItem("yummyPrepUser") || localStorage.getItem("yummyPrepUser");
            var user = raw ? JSON.parse(raw) : null;
            return user && user.type !== "admin" ? user : null;
        } catch (e) {
            return null;
        }
    }

    function storeUser(user) {
        try {
            var target = localStorage.getItem("yummyPrepUser") ? localStorage : sessionStorage;
            target.setItem("yummyPrepLoggedIn", "true");
            target.setItem("yummyPrepUser", JSON.stringify(user));
        } catch (e) { /* storage unavailable */ }
    }

    function clearStoredUser() {
        KEYS.forEach(function (k) {
            try { localStorage.removeItem(k); sessionStorage.removeItem(k); } catch (e) { }
        });
    }


    /* ---------- verify against the PHP session ---------- */

    var pending = null;

    function verifySession() {
        if (!pending) {
            pending = fetch(API_URL + "?action=check_session", { credentials: "same-origin" })
                .then(function (r) { return r.json(); })
                .then(function (res) {
                    if (res.status === "success" && res.data && res.data.type === "customer") {
                        storeUser(res.data);
                        return res.data;
                    }
                    clearStoredUser();
                    return null;
                })
                .catch(function () {
                    // Server unreachable: trust what the browser remembers
                    return readStoredUser();
                });
        }
        return pending;
    }

    window.YummyAuth = {
        getUser: function (callback) {
            var stored = readStoredUser();
            if (stored) callback(stored);
            verifySession().then(function (user) {
                if (user && (!stored || JSON.stringify(user) !== JSON.stringify(stored))) callback(user);
            });
        }
    };


    /* ---------- navbar ---------- */

    function firstName(user) {
        var name = (user.name || user.full_name || user.email || "Account").trim();
        return name.split(" ")[0];
    }

    function renderNavbar(user) {
        var box = document.querySelector(".nav-buttons");
        if (!box) return;

        var links = Array.prototype.slice.call(box.querySelectorAll("a.login-btn"));
        var loginLink = null;

        links.forEach(function (a) {
            var text = a.textContent.trim().toLowerCase();
            if (text === "login" || text.indexOf("hi,") === 0) {
                if (!loginLink) loginLink = a;
            } else if (text === "register") {
                a.style.display = user ? "none" : "";
            }
        });

        if (!loginLink) return;

        var existingLogout = box.querySelector(".logout-btn");

        if (user) {
            loginLink.textContent = "Hi, " + firstName(user);
            loginLink.setAttribute("href", PROFILE_URL);
            loginLink.setAttribute("title", "View my profile");
            loginLink.classList.add("account-btn");

            if (!existingLogout) {
                var logout = document.createElement("a");
                logout.href = "#";
                logout.className = "login-btn logout-btn";
                logout.textContent = "Logout";
                logout.addEventListener("click", function (e) {
                    e.preventDefault();
                    fetch(API_URL + "?action=logout", { credentials: "same-origin" })
                        .catch(function () { })
                        .then(function () {
                            clearStoredUser();
                            window.location.href = LOGIN_URL;
                        });
                });
                loginLink.insertAdjacentElement("afterend", logout);
            }
        } else {
            loginLink.textContent = "Login";
            loginLink.setAttribute("href", LOGIN_URL);
            loginLink.removeAttribute("title");
            loginLink.classList.remove("account-btn");
            if (existingLogout) existingLogout.remove();
        }
    }

    function init() {
        var stored = readStoredUser();
        if (stored) renderNavbar(stored);          // instant, no flicker
        verifySession().then(renderNavbar);        // then confirm with the server
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }

})();
