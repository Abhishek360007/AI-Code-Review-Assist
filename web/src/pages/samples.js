// Deliberately flawed snippets so new users can see what a review looks like.

export const SAMPLES = {
  javascript: `// Fetch a user's orders and compute the total
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
  python: `import sqlite3

def find_user(name):
    conn = sqlite3.connect("app.db")
    cur = conn.cursor()
    cur.execute("SELECT * FROM users WHERE name = '" + name + "'")
    try:
        return cur.fetchall()[0]
    except:
        pass

def average(values=[]):
    return sum(values) / len(values)
`,
  java: `import java.util.*;

public class ReportService {
    private static final String PASSWORD = "admin123";

    public String buildReport(List<String> rows) {
        String out = "";
        for (int i = 0; i < rows.size(); i++) {
            out += rows.get(i) + "\\n";
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
        return out;
    }
}
`,
}
