<div align="center">
  <img src="https://github.com/Flummidill/SimpleHomes/blob/HEAD/icons/SimpleHomes-250x250.png?raw=true" alt="SimpleHomes-Icon">
  
  <h1>SimpleHomes</h1>
  <a href="https://modrinth.com/plugin/simple_homes/versions">
    <img src="https://img.shields.io/modrinth/v/simple_homes?style=for-the-badge&label=Version&color=5A00FF">
    <img src="https://img.shields.io/modrinth/dt/simple_homes?style=for-the-badge&label=Downloads&color=29A100">
  </a>
</div>


## 🎯 Features
- **Easily set Homes**  
Players can set, teleport to, and delete their own homes, making navigation and survival much more convenient.

####

- **Admin Home Management**  
Admins can set, teleport to, delete, and adjust the max number of homes for any player, giving full control when assisting or moderating.

####

- **Customizable Home Limits**  
Define how many homes players can set by default, keeping balance between convenience and fairness on your server.


<hr/>


### Commands:
```
/sethome <number> - Set a Home
/home <number> - Teleport to a Home
/delhome <number> - Delete a Home
```

### Admin Commands:
```
/homeadmin sethome <player> <number> - Set another Player's Home
/homeadmin home <player> <number> - Teleport to another Player's Home
/homeadmin delhome <player> <number> - Delete another Player's Home
/homeadmin maxhomes <player> <number> - Set the Maximum Number of Homes a Player can set
```

### Permissions:
```
simplehomes.use (Default: true) - Allow use of Player Commands
simplehomes.admin (Default: false) - Allow use of Admin Commands and bypassing Home-Limits
```

### Config:
```
max-homes - Set the default Maximum Number of Homes a Player can set
admin-tp-delay - Set if Admins should be able to teleport to Homes instantly when using /homeadmin
```
