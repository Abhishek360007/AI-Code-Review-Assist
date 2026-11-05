// Demo snippets, one per language. Each is deliberately flawed in a different, realistic way
// (security, crashes, leaks, concurrency, performance) so a review always has something to show.
// Note: inside these template literals, \\n is a literal backslash-n and \` / \${ are escaped.

export const SAMPLES = {
  javascript: {
    title: 'Order total calculator',
    code: `// Fetch a user's orders and compute the total
async function getOrderTotal(db, userId) {
  var apiKey = "sk-live-9f8e7d6c5b4a";
  const rows = await db.query("SELECT * FROM orders WHERE user_id = " + userId);
  let total = 0;
  for (let i = 0; i <= rows.length; i++) {
    total += rows[i].amount;
  }
  if (total == "0") {
    console.log("no orders for", userId);
  }
  return total;
}
`,
  },

  typescript: {
    title: 'User cache and welcome emails',
    code: `interface User { id: number; name: string; email?: string }

declare const mailer: { send(to: string, body: string): Promise<void> };

const cache: any = {};

export async function getUser(id: number): Promise<User> {
  if (cache[id]) return cache[id];
  const res = await fetch("http://api.example.com/users/" + id);
  const user = res.json();
  cache[id] = user;
  return user;
}

export function sendWelcome(users: User[]) {
  users.forEach(async (u) => {
    await mailer.send(u.email!.toLowerCase(), "Welcome, " + u.name);
  });
  console.log("All welcome emails sent");
}
`,
  },

  python: {
    title: 'User lookup and helpers',
    code: `import sqlite3
import pickle

def find_user(name):
    conn = sqlite3.connect("app.db")
    cur = conn.cursor()
    cur.execute("SELECT * FROM users WHERE name = '" + name + "'")
    try:
        return cur.fetchall()[0]
    except:
        pass

def load_session(cookie_bytes):
    return pickle.loads(cookie_bytes)

def average(values=[]):
    return sum(values) / len(values)

def add_tag(tag, tags=[]):
    tags.append(tag)
    return tags
`,
  },

  java: {
    title: 'User repository',
    code: `import java.sql.*;
import java.util.*;

public class UserRepository {
    private static final String DB_PASSWORD = "admin123";

    public List<String> findEmails(String role) throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/app", "root", DB_PASSWORD);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT email FROM users WHERE role = '" + role + "'");
        List<String> emails = new ArrayList<>();
        while (rs.next()) {
            String email = rs.getString("email");
            if (email != "") {
                emails.add(email.toLowerCase());
            }
        }
        return emails;
    }

    public String buildReport(List<String> rows) {
        String out = "";
        for (String row : rows) {
            out += row + "\\n";
        }
        return out;
    }
}
`,
  },

  cpp: {
    title: 'Buffer and vector utilities',
    code: `#include <cstring>
#include <iostream>
#include <vector>

class Buffer {
public:
    Buffer(size_t n) { data = new char[n]; size = n; }
    ~Buffer() { delete data; }
    char* data;
    size_t size;
};

void greet(const char* input) {
    char name[16];
    strcpy(name, input);
    std::cout << "Hello, " << name << std::endl;
}

int sum(const std::vector<int>& v) {
    int total = 0;
    for (int i = 0; i <= v.size(); i++) {
        total += v[i];
    }
    return total;
}

Buffer* makeBuffer() {
    Buffer b(64);
    return &b;
}
`,
  },

  c: {
    title: 'Line reader and logger',
    code: `#include <stdio.h>
#include <stdlib.h>
#include <string.h>

char *read_line(void) {
    char buf[64];
    gets(buf);
    char *copy = malloc(strlen(buf));
    strcpy(copy, buf);
    return copy;
}

void log_message(const char *msg) {
    printf(msg);
}

int main(void) {
    for (int i = 0; i < 10; i++) {
        char *line = read_line();
        log_message(line);
    }
    return 0;
}
`,
  },

  csharp: {
    title: 'Order service',
    code: `using System;
using System.Data.SqlClient;
using System.Net.Http;

public class OrderService
{
    private const string ConnStr = "Server=prod;Database=shop;User Id=sa;Password=P@ssw0rd;";

    public async void SyncOrders()
    {
        var client = new HttpClient();
        var json = await client.GetStringAsync("http://partner.example.com/orders");
        Console.WriteLine(json);
    }

    public decimal GetTotal(string customerId)
    {
        var conn = new SqlConnection(ConnStr);
        conn.Open();
        var cmd = new SqlCommand(
            "SELECT SUM(Amount) FROM Orders WHERE CustomerId = '" + customerId + "'", conn);
        try
        {
            return (decimal)cmd.ExecuteScalar();
        }
        catch (Exception) { return 0; }
    }
}
`,
  },

  go: {
    title: 'Concurrent fetcher and logger',
    code: `package main

import (
\t"fmt"
\t"io"
\t"net/http"
\t"os"
)

var hits = 0

func fetchAll(urls []string) {
\tfor _, u := range urls {
\t\tgo func() {
\t\t\tresp, _ := http.Get(u)
\t\t\tdefer resp.Body.Close()
\t\t\tbody, _ := io.ReadAll(resp.Body)
\t\t\thits++
\t\t\tfmt.Println(u, len(body))
\t\t}()
\t}
}

func writeLog(lines []string) {
\tfor _, l := range lines {
\t\tf, _ := os.OpenFile("app.log", os.O_APPEND|os.O_WRONLY, 0644)
\t\tdefer f.Close()
\t\tf.WriteString(l + "\\n")
\t}
}
`,
  },

  rust: {
    title: 'Config loader and helpers',
    code: `use std::collections::HashMap;
use std::fs;

fn load_config(path: &str) -> HashMap<String, String> {
    let text = fs::read_to_string(path).unwrap();
    let mut map = HashMap::new();
    for line in text.lines() {
        let parts: Vec<&str> = line.split('=').collect();
        map.insert(parts[0].to_string(), parts[1].to_string());
    }
    map
}

fn longest(words: &Vec<String>) -> String {
    let mut best = words[0].clone();
    for w in words.clone() {
        if w.len() > best.len() {
            best = w.clone();
        }
    }
    best
}

fn first_byte(s: &str) -> u8 {
    unsafe { *s.as_ptr() }
}
`,
  },

  kotlin: {
    title: 'Profile repository',
    code: `import kotlinx.coroutines.*
import java.sql.DriverManager

class ProfileRepository {
    lateinit var userName: String

    fun loadProfile(id: String): String {
        val conn = DriverManager.getConnection("jdbc:postgresql://localhost/app", "admin", "admin")
        val rs = conn.createStatement().executeQuery("SELECT bio FROM profiles WHERE id = '$id'")
        rs.next()
        return rs.getString("bio")!!
    }

    fun refreshAll(ids: List<String>) {
        for (id in ids) {
            GlobalScope.launch {
                println(loadProfile(id).uppercase())
            }
        }
    }

    fun greeting() = "Hello, " + userName.trim()
}
`,
  },

  php: {
    title: 'Product page and sign-up',
    code: `<?php
$conn = mysqli_connect("localhost", "root", "", "shop");

$id = $_GET['id'];
$result = mysqli_query($conn, "SELECT * FROM products WHERE id = $id");
$product = mysqli_fetch_assoc($result);

echo "<h1>" . $product['name'] . "</h1>";
echo "<p>You searched for: " . $_GET['q'] . "</p>";

function register($username, $password) {
    global $conn;
    $hash = md5($password);
    mysqli_query($conn, "INSERT INTO users VALUES ('$username', '$hash')");
}

if (isset($_POST['username'])) {
    register($_POST['username'], $_POST['password']);
    header("Location: " . $_GET['next']);
}
`,
  },

  ruby: {
    title: 'Orders controller',
    code: `class OrdersController < ApplicationController
  def index
    @orders = Order.all
    @orders.each do |order|
      puts order.customer.name
    end
  end

  def create
    @order = Order.new(params[:order])
    @order.save rescue nil
    redirect_to params[:return_to]
  end

  def report
    status = params[:status]
    @results = Order.where("status = '#{status}'")
    render plain: eval(params[:format_expr])
  end
end
`,
  },

  swift: {
    title: 'Profile view controller',
    code: `import UIKit

class ProfileViewController: UIViewController {
    var label: UILabel!
    var onUpdate: (() -> Void)?

    override func viewDidLoad() {
        super.viewDidLoad()
        onUpdate = {
            self.label.text = "Updated"
        }
        loadAvatar(from: "http://example.com/avatar.png")
    }

    func loadAvatar(from urlString: String) {
        let url = URL(string: urlString)!
        URLSession.shared.dataTask(with: url) { data, _, _ in
            let image = UIImage(data: data!)
            self.view.backgroundColor = UIColor(patternImage: image!)
        }.resume()
    }

    func readSettings() -> [String: Any] {
        let data = try! Data(contentsOf: URL(fileURLWithPath: "settings.json"))
        return try! JSONSerialization.jsonObject(with: data) as! [String: Any]
    }
}
`,
  },

  sql: {
    title: 'Revenue report and cleanup',
    code: `-- Monthly revenue report
SELECT *
FROM orders o, customers c
WHERE o.customer_id = c.id
  AND YEAR(o.created_at) = 2026
  AND c.email LIKE '%@gmail.com'
ORDER BY o.created_at;

-- Deactivate churned customers
UPDATE customers SET status = 'inactive';

-- Remove stale sessions
DELETE FROM sessions
WHERE user_id IN (SELECT id FROM users WHERE last_login = NULL);

CREATE TABLE payments (
  id INT,
  card_number VARCHAR(16),
  cvv VARCHAR(3),
  amount FLOAT
);
`,
  },

  shell: {
    title: 'Deploy script',
    code: `#!/bin/bash
# Deploy the app and notify the team
BACKUP_DIR=$1
API_TOKEN="ghp_live_51fa9c2e7d0b"

cd /var/www/app
git pull
rm -rf $BACKUP_DIR/*

for f in $(ls *.log); do
  cat $f | grep ERROR >> errors.txt
done

curl -k -H "Authorization: $API_TOKEN" http://deploy.example.com/notify
chmod -R 777 /var/www/app
eval "$2"
`,
  },

  plaintext: {
    title: 'Dockerfile',
    code: `# Dockerfile for the web app
FROM node:latest
COPY . .
RUN npm install
ENV DATABASE_PASSWORD=supersecret
EXPOSE 3000
USER root
CMD npm start
`,
  },
}

const SAMPLE_CODES = new Set(Object.values(SAMPLES).map((s) => s.code))
const SAMPLE_TITLES = new Set(Object.values(SAMPLES).map((s) => s.title))

/** True when the editor holds an untouched sample (or nothing), so it's safe to replace. */
export const isSampleOrEmpty = (code) => !code.trim() || SAMPLE_CODES.has(code)
export const isSampleTitle = (title) => !title || SAMPLE_TITLES.has(title)
