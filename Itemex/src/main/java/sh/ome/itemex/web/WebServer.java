package sh.ome.itemex.web;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import sh.ome.itemex.Itemex;
import sh.ome.itemex.commands.commands;
import sh.ome.itemex.files.CategoryFile;
import sh.ome.itemex.functions.sqliteDb;
import sh.ome.itemex.functions.sqliteDb.OrderBuffer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebServer {

    private HttpServer server;
    private final Itemex plugin;
    private final int port;

    public WebServer(Itemex plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            // Use a single root handler to manage routing manually and support proxy prefixes
            server.createContext("/", new RootHandler());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            plugin.getLogger().info("Web server started on port " + port);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not start web server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web server stopped");
        }
    }

    class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            
            // Simple routing based on path suffix to handle proxy prefixes (e.g. /itemex/api/items)
            if (path.endsWith("/api/items") || path.endsWith("/api/items/")) {
                new ItemsHandler().handle(t);
            } else if (path.contains("/api/history")) {
                new HistoryHandler().handle(t);
            } else if (path.contains("/api/orders")) {
                new OrdersHandler().handle(t);
            } else if (path.contains("/api/config")) {
                new ConfigHandler().handle(t);
            } else {
                // Default to static handler (HTML) for root or unknown paths
                new StaticHandler().handle(t);
            }
        }
    }

    class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // plugin.getLogger().info("Web Request: " + t.getRequestURI());
            
            // Use relative paths for API calls (api/items instead of /api/items)
            String response = "<!DOCTYPE html><html lang=\"en\" data-bs-theme=\"dark\"><head><title>Itemex Exchange</title>" +
                    "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" +
                    "<link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css\" rel=\"stylesheet\">" +
                    "<script src=\"https://unpkg.com/lightweight-charts@4.1.1/dist/lightweight-charts.standalone.production.js\"></script>" +
                    "<style>" +
                    "body{height:100vh;overflow:hidden;display:flex;flex-direction:column;}" +
                    ".mobile-header{display:none;}" +
                    "#main-container{flex:1;overflow:hidden;padding:0;}" +
                    ".row{height:100%;margin:0;}" +
                    "#sidebar{height:100%;display:flex;flex-direction:column;border-right:1px solid #495057;}" +
                    "#item-list-container{flex:1;overflow-y:auto;}" +
                    "#content{height:100%;overflow-y:auto;padding:20px;}" +
                    "#orderbook{height:100%;overflow-y:auto;border-left:1px solid #495057;}" +
                    "@media (max-width: 768px) {" +
                    "  .mobile-header { display: flex; align-items: center; padding: 10px; background-color: #212529; border-bottom: 1px solid #495057; flex: 0 0 auto; }" +
                    "  #sidebar { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; z-index: 1050; background-color: #212529; }" +
                    "  #sidebar.show { display: flex; }" +
                    "  #content { height: 60%; padding-top: 10px; }" +
                    "  #orderbook { height: 40%; border-left: none; border-top: 1px solid #495057; }" +
                    "}" +
                    "</style>" +
                    "</head><body>" +
                    
                    // Mobile Header
                    "<div class=\"mobile-header\">" +
                    "<button class=\"btn btn-outline-secondary w-100 text-start\" onclick=\"openSidebar()\">🔍 Search items...</button>" +
                    "</div>" +

                    "<div class=\"container-fluid\" id=\"main-container\">" +
                    "<div class=\"row\">" +
                    
                    // Sidebar
                    "<div class=\"col-md-2 p-0 bg-body-tertiary\" id=\"sidebar\">" +
                    "<div class=\"p-3 border-bottom border-secondary d-flex justify-content-between align-items-center\">" +
                    "<h5>Commodities</h5>" +
                    "<button class=\"btn btn-close btn-close-white d-md-none\" onclick=\"closeSidebar()\"></button>" +
                    "</div>" +
                    "<div class=\"px-3\">" +
                    "<input type=\"text\" id=\"search-input\" class=\"form-control form-control-sm bg-dark text-light border-secondary mt-2\" placeholder=\"Search items...\">" +
                    "</div>" +
                    "<div class=\"list-group list-group-flush mt-2\" id=\"item-list-container\">" +
                    "<div id=\"item-list\"><div class=\"text-center p-3 text-muted\">Loading...</div></div>" +
                    "</div></div>" +
                    
                    // Main Content (Chart)
                    "<div class=\"col-md-7\" id=\"content\">" +
                    "<h2 id=\"chart-title\" class=\"mb-4\">Select an item</h2>" +
                    "<div id=\"chart-container\" style=\"position: relative; height: 60vh; width: 100%;\"></div>" +
                    "</div>" +
                    
                    // Order Book
                    "<div class=\"col-md-3 bg-body-tertiary p-0\" id=\"orderbook\">" +
                    "<div class=\"p-3 border-bottom border-secondary\"><h5>Order Book</h5></div>" +
                    "<div class=\"p-2\">" +
                    "<h6 class=\"text-danger\">Asks (Sell)</h6>" +
                    "<table class=\"table table-sm table-dark table-striped\" style=\"font-size: 0.8rem;\">" +
                    "<thead><tr><th>Price</th><th class=\"text-end\">Amount</th></tr></thead>" +
                    "<tbody id=\"asks-body\"><tr><td colspan=\"2\" class=\"text-muted text-center\">-</td></tr></tbody>" +
                    "</table>" +
                    "<h6 class=\"text-success mt-3\">Bids (Buy)</h6>" +
                    "<table class=\"table table-sm table-dark table-striped\" style=\"font-size: 0.8rem;\">" +
                    "<thead><tr><th>Price</th><th class=\"text-end\">Amount</th></tr></thead>" +
                    "<tbody id=\"bids-body\"><tr><td colspan=\"2\" class=\"text-muted text-center\">-</td></tr></tbody>" +
                    "</table>" +
                    "</div>" +
                    "</div>" +
                    
                    "</div></div>" +
                    "<script>" +
                    "function openSidebar() { document.getElementById('sidebar').classList.add('show'); document.getElementById('search-input').focus(); }" +
                    "function closeSidebar() { document.getElementById('sidebar').classList.remove('show'); }" +
                    "\n" +
                    "let config = { currencySymbol: '$', decimals: 2, decimal_separator: '.', thousand_separator: ',', unitLocation: 'left' };\n" +
                    "// Fetch config\n" +
                    "fetch('api/config').then(r => r.json()).then(c => { config = c; });\n" +
                    "\n" +
                    "function formatPrice(price) {\n" +
                    "  let p = parseFloat(price).toFixed(config.decimals);\n" +
                    "  let parts = p.split('.');\n" +
                    "  parts[0] = parts[0].replace(/\\B(?=(\\d{3})+(?!\\d))/g, config.thousand_separator);\n" +
                    "  let formatted = parts.join(config.decimal_separator);\n" +
                    "  if (config.unitLocation === 'right') return formatted + config.currencySymbol;\n" +
                    "  return config.currencySymbol + formatted;\n" +
                    "}\n" +
                    "\n" +
                    "let allItems = [];\n" +
                    "// Fetch items using relative path\n" +
                    "fetch('api/items')" +
                    "  .then(r => r.json())" +
                    "  .then(items => {" +
                    "    allItems = items;" +
                    "    renderList(items);" +
                    "  }).catch(e => {" +
                    "    console.error('Fetch error:', e);" +
                    "    document.getElementById('item-list').innerHTML = '<div class=\"text-center p-3 text-danger\">Error loading items</div>';" +
                    "  });" +
                    "\n" +
                    "// Search filter\n" +
                    "document.getElementById('search-input').addEventListener('input', (e) => {" +
                    "  const term = e.target.value.toLowerCase();" +
                    "  const filtered = allItems.filter(i => i.name.toLowerCase().includes(term));" +
                    "  renderList(filtered);" +
                    "});" +
                    "\n" +
                    "function renderList(items) {" +
                    "  const list = document.getElementById('item-list');" +
                    "  list.innerHTML = '';" +
                    "  if (items.length === 0) { list.innerHTML = '<div class=\"text-center p-3 text-muted\">No items found</div>'; return; }" +
                    "  const limit = 500;" +
                    "  items.slice(0, limit).forEach(item => {" +
                    "    const a = document.createElement('a');" +
                    "    a.className = 'list-group-item list-group-item-action item-row bg-transparent text-light border-secondary';" +
                    "    let priceHtml = '';" +
                    "    if (item.price) {" +
                    "      priceHtml = `<span class='badge bg-secondary'>${formatPrice(item.price)}</span>`;" +
                    "    }" +
                    "    a.innerHTML = `<div class='d-flex justify-content-between align-items-center'><strong>${item.name}</strong>${priceHtml}</div>`;" +
                    "    a.onclick = () => loadItem(item.itemid, item.name);" +
                    "    list.appendChild(a);" +
                    "  });" +
                    "}" +
                    "\n" +
                    "// Chart Setup\n" +
                    "const chartContainer = document.getElementById('chart-container');" +
                    "let chart;" +
                    "let lineSeries;" +
                    "try {" +
                    "  chart = LightweightCharts.createChart(chartContainer, {" +
                    "    layout: { textColor: '#adb5bd', background: { type: 'solid', color: '#212529' } }," +
                    "    grid: { vertLines: { color: '#495057' }, horzLines: { color: '#495057' } }," +
                    "    timeScale: { timeVisible: true, borderColor: '#495057' }," +
                    "    rightPriceScale: { borderColor: '#495057' }" +
                    "  });" +
                    "  lineSeries = chart.addLineSeries({ color: '#0d6efd' });" +
                    "} catch (err) {" +
                    "  console.error('Chart initialization failed:', err);" +
                    "  document.getElementById('chart-container').innerHTML = '<div class=\"alert alert-danger\">Chart failed to load</div>';" +
                    "}" +
                    "\n" +
                    "let currentItem = '';" +
                    "function loadItem(itemid, name) {" +
                    "  currentItem = itemid;" +
                    "  if (window.innerWidth < 768) closeSidebar();" +
                    "  document.getElementById('chart-title').textContent = name + ' Price History';" +
                    "  \n" +
                    "  // Load History\n" +
                    "  if (lineSeries) {" +
                    "    fetch('api/history?item=' + encodeURIComponent(itemid)).then(r => r.json()).then(data => {" +
                    "      const chartData = data.map(d => ({ time: d.time, value: d.price }));" +
                    "      chartData.sort((a, b) => a.time - b.time);" +
                    "      lineSeries.setData(chartData);" +
                    "      chart.timeScale().fitContent();" +
                    "    });" +
                    "  }" +
                    "  \n" +
                    "  // Load Order Book\n" +
                    "  fetch('api/orders?item=' + encodeURIComponent(itemid)).then(r => r.json()).then(data => {" +
                    "    const asksBody = document.getElementById('asks-body');" +
                    "    const bidsBody = document.getElementById('bids-body');" +
                    "    asksBody.innerHTML = '';" +
                    "    bidsBody.innerHTML = '';" +
                    "    \n" +
                    "    if (data.asks.length === 0) asksBody.innerHTML = '<tr><td colspan=\"2\" class=\"text-muted text-center\">No asks</td></tr>';" +
                    "    else data.asks.forEach(o => {" +
                    "      asksBody.innerHTML += `<tr><td class=\"text-danger\">${formatPrice(o.price)}</td><td class=\"text-end\">${o.amount}</td></tr>`;" +
                    "    });" +
                    "    \n" +
                    "    if (data.bids.length === 0) bidsBody.innerHTML = '<tr><td colspan=\"2\" class=\"text-muted text-center\">No bids</td></tr>';" +
                    "    else data.bids.forEach(o => {" +
                    "      bidsBody.innerHTML += `<tr><td class=\"text-success\">${formatPrice(o.price)}</td><td class=\"text-end\">${o.amount}</td></tr>`;" +
                    "    });" +
                    "  });" +
                    "}" +
                    "window.onresize = function() { if(chart) chart.resize(chartContainer.clientWidth, chartContainer.clientHeight); };" +
                    "</script></body></html>";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    class ItemsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // plugin.getLogger().info("API Request: /api/items");
            List<Map<String, String>> items = new ArrayList<>();
            
            try {
                // Force reload to ensure we have the latest data
                if (CategoryFile.get() == null) {
                    CategoryFile.setup();
                } else {
                    CategoryFile.reload();
                }
                
                if (CategoryFile.get() != null) {
                    List<String> categories = CategoryFile.get().getStringList("categories.CATEGORY_NAMES");
                    
                    if (categories != null) {
                        for (String cat : categories) {
                            String[] category = cat.split(":", 0);
                            if (category.length > 0) {
                                List<String> catItems = CategoryFile.get().getStringList("categories." + category[0]);
                                if (catItems != null) {
                                    for (String item_json : catItems) {
                                        try {
                                            String itemName = commands.get_meta(item_json);
                                            Map<String, String> itemData = new HashMap<>();
                                            itemData.put("name", itemName);
                                            itemData.put("itemid", item_json);
                                            
                                            // Fetch last price
                                            String lastPriceRaw = sqliteDb.getLastPrice(item_json);
                                            if (lastPriceRaw != null && !lastPriceRaw.equals("0")) {
                                                String[] parts = lastPriceRaw.split(":");
                                                if (parts.length > 0) {
                                                    itemData.put("price", parts[0]);
                                                }
                                            }
                                            
                                            items.add(itemData);
                                        } catch (Exception e) {
                                            // ignore bad items
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Exception in ItemsHandler: " + e.getMessage());
                e.printStackTrace();
            }
            
            Gson gson = new Gson();
            String json = gson.toJson(items);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            String itemid = null;
            if (query != null && query.contains("item=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("item")) {
                        itemid = URLDecoder.decode(pair[1], "UTF-8");
                        break;
                    }
                }
            }

            if (itemid != null) {
                List<String> history = sqliteDb.getAllFulfilledOrders(itemid);
                List<Map<String, Object>> data = new ArrayList<>();
                for (String entry : history) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2) {
                        Map<String, Object> point = new HashMap<>();
                        try {
                            point.put("price", Double.parseDouble(parts[0]));
                            point.put("time", Long.parseLong(parts[1]));
                            data.add(point);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                Gson gson = new Gson();
                String json = gson.toJson(data);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                t.getResponseHeaders().set("Content-Type", "application/json");
                t.sendResponseHeaders(200, bytes.length);
                OutputStream os = t.getResponseBody();
                os.write(bytes);
                os.close();
            } else {
                String response = "Missing item parameter";
                t.sendResponseHeaders(400, response.length());
                OutputStream os = t.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    class OrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            String itemid = null;
            if (query != null && query.contains("item=")) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1 && pair[0].equals("item")) {
                        itemid = URLDecoder.decode(pair[1], "UTF-8");
                        break;
                    }
                }
            }

            Map<String, List<OrderBuffer>> orders = new HashMap<>();
            if (itemid != null) {
                // Fetch sells (Asks) - Sorted by Price ASC
                List<OrderBuffer> sells = sqliteDb.selectAll("SELLORDERS", itemid);
                // Fetch buys (Bids) - Sorted by Price DESC
                List<OrderBuffer> buys = sqliteDb.selectAll("BUYORDERS", itemid);
                
                // Limit to top 50 to avoid huge payloads
                if (sells.size() > 50) sells = sells.subList(0, 50);
                if (buys.size() > 50) buys = buys.subList(0, 50);

                orders.put("asks", sells);
                orders.put("bids", buys);
            } else {
                orders.put("asks", new ArrayList<>());
                orders.put("bids", new ArrayList<>());
            }

            Gson gson = new Gson();
            String json = gson.toJson(orders);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    class ConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Map<String, Object> config = new HashMap<>();
            config.put("currencySymbol", Itemex.currencySymbol);
            config.put("decimals", Itemex.decimals);
            config.put("decimal_separator", String.valueOf(Itemex.decimal_separator));
            config.put("thousand_separator", String.valueOf(Itemex.thousand_separator));
            config.put("unitLocation", Itemex.unitLocation);

            Gson gson = new Gson();
            String json = gson.toJson(config);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
