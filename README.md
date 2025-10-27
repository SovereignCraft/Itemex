# Itemex

**Itemex** is a free market plugin for Minecraft Spigot/Paper servers that simulates a stock or crypto exchange for items. Prices are dynamically determined by **supply and demand**, allowing players to buy/sell **any item** at market or limit prices. Maximize profits, strategize trades, and experience realistic economics! 
**Languages:** DE, ES, FR, CN, RU

## ✨ Features
- **Dynamic Pricing**: Prices fluctuate based on supply (sell orders) and demand (buy orders) – make revenues or losses!
- **Multiple Interfaces**:
    - **GUI**: `/ix gui` or `/i gui` (user-friendly)
    - **Commands**: Full CLI support
    - **Sign Shops**: `[ix]` for market orders
    - **Chest Shops**: `[ixc]` (temporarily deactivated)
    - **WebUI**: Planned
- **Order Types**:
    - **Market Orders**: Buy/sell instantly at current price
    - **Limit Orders**: Set your price and wait for matches
- **Full Item Support**: All items, including goat horns, suspicious stew, paintings, multi-enchanted items
- **Quick Actions**: `/ix fastsell` – sell entire inventory at market price
- **Economy Integration**: Vault-compatible
- **Resource Allocation**: Prices signal high-demand items for strategic play
- **Admin Tools**: Configurable fees, interventions, backups

## 📸 Screenshots
![Sign Shop Example](https://ipfs.ome.sh/ipfs/QmUbbuHEnk6DsmmqeXNRp4MtRRFUzC87cD6y8uhbdNg6CZ/)

*(More screenshots available on [SpigotMC page](https://www.spigotmc.org/resources/itemex-item-exchange-free-market-plugin-like-a-stock-or-crypto-exchange-with-mc-items.108398/))*

- GUI Interface
- Command Line Usage
- Sign & Chest Shop Setup

## 🚀 Installation
1. **Dependencies**: Install [Vault](https://www.spigotmc.org/resources/vault.34315/) and [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) (soft-depend)
5. Edit `plugins/Itemex/config.yml` if needed and `/reload`

**Supported Versions**:
- Native: **1.20**
- Tested: **1.19–1.20**  

## 💻 Commands
| Command | Aliases | Description | Example |
|---------|---------|-------------|---------|
| `/ix` | `/i` | Main menu/help | `/ix` |
| `/ix gui` | `/i gui` | Open user-friendly GUI | `/ix gui` |
| `/ix price` | - | View current item price | `/ix price` |
| `/ix buy <amount> [price]` | - | Buy items (market/limit) | `/ix buy 64` |
| `/ix sell <amount> [price]` | - | Sell items (market/limit) | `/ix sell 64` |
| `/ix fastsell` | - | Sell all inventory items at market | `/ix fastsell` |
| `/ix deposit` | - | Deposit items/money | `/ix deposit` |
| `/ix withdraw` | `/ix send` | Withdraw items/money | `/ix withdraw` |
| `/ix order list` | - | List your orders | `/ix order list` |
| `/ix order close <id>` | - | Cancel order | `/ix order close 1` |
| `/ix whatIsInMyRightHand` | - | Check hand item price | `/ix whatIsInMyRightHand` |

## 🔑 Permissions
Prefix: `itemex.command.`
| Permission | Description | Default |
|------------|-------------|---------|
| `itemex.use` | Basic usage | `true` |
| `itemex.command.ix.*` | All `/ix` subcommands | `op` |
| `itemex.command.ix.help` | Help command | `true` |
| `itemex.command.ix.price` | View prices | `true` |
| `itemex.command.ix.buy` | Buy orders | `true` |
| `itemex.command.ix.sell` | Sell orders | `true` |
| `itemex.command.ix.gui` | GUI access | `true` |


## ⚙️ Configuration

Key options:
- `debug: false`
- Database: SQLite (default), MySQL/PostgreSQL support
- Update intervals, max items, auto-save/backup
- Messages & prefixes
- Metrics & update checker

## 📄 License
[MIT License](https://github.com/xcatpc/Itemex/blob/main/LICENSE) – Free to use/modify.