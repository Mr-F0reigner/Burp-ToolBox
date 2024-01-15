package Extension.ContextMenu;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import main.ToolBox;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GeneratePOC {
    private ContextMenuEvent event;
    private List<Component> menuItemList;

    public GeneratePOC(ContextMenuEvent event, List<Component> menuItemList) {
        this.event = event;
        this.menuItemList = menuItemList;
    }

    public void PythonPoC() {
        // 创建上下文菜单
        JMenuItem PythonPoC = new JMenuItem("Python PoC");
        // 菜单点击事件
        PythonPoC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        menuItemList.add(PythonPoC);
    }

    // 保存文件操作
    private void saveToFile() {
        HttpRequest request = event.messageEditorRequestResponse().get().requestResponse().request();
        // 文件选择器设置
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save As");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Python (*.py)", "py"));
        fileChooser.setAcceptAllFileFilterUsed(true);

        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // 文件路径处理
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();

            // 检查文件名是否以 .py 结尾，如果不是则添加 .py 后缀
            if (!filePath.toLowerCase().endsWith(".py")) {
                fileToSave = new File(filePath + ".py");
            }

            String path = request.url().toString();
            List<HttpHeader> headers = request.headers();
            String pythonScript = "";
            // 输出对应请求类型的PoC模板
            try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
                if (request.method().equals("GET")) {
                    pythonScript = generatePythonScript("GET", path, null, headers);
                } else if (request.method().equals("POST")) {
                    String body = request.bodyToString();
                    pythonScript = generatePythonScript("POST", path, body, headers);
                }
                fileWriter.write(pythonScript);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String generatePythonScript(String method, String path, String body, List<HttpHeader> headers) {
        StringBuilder script = new StringBuilder();
        script.append("import requests\n");
        script.append("from requests.exceptions import RequestException\n");
        script.append("import argparse\n");
        script.append("import threading\n\n");

        // 添加 PoC 函数
        script.append("def PoC(host):\n");
        path = path.replaceAll("(?<=(http:\\/\\/)|(https:\\/\\/)).*?(?=\\/)", "{host}");
        script.append("    url = f\'").append(path).append("\'\n");
        script.append("    headers = {\n");
        for (HttpHeader header : headers) {
            if (header.name().equals("Host")) {
                script.append("        \'").append(header.name()).append("\': ").append("host").append(",\n");
            } else {
                script.append("        \'").append(header.name()).append("\': \'").append(header.value()).append("\',\n");
            }
        }
        script.append("    }\n");

        if (method.equals("POST")) {
            script.append("    data = \"\"\"");
            body.lines().forEach(line -> script.append(line).append("\\r\n"));
            script.append("\"\"\"\n");
        }

        script.append("    try:\n");
        if (method.equals("GET")) {
            script.append("        resp = requests.").append(method.toLowerCase()).append("(url, headers=headers, allow_redirects=True, timeout=8)\n");
        } else {
            script.append("        resp = requests.").append(method.toLowerCase()).append("(url, headers=headers, data=data,allow_redirects=True, timeout=8)\n");
        }
        script.append("        print(\"Status Code: \" + str(resp.status_code))\n");
        script.append("        print(resp.text)\n");
        script.append("    except RequestException as e:\n");
        script.append("        print(f\"{host} Connection failed: {e}\")\n\n");

        // 添加 process_hosts 函数
        script.append("def process_hosts(filename, thread_count):\n");
        script.append("    with open(filename, \"r\") as file:\n");
        script.append("        hosts = file.read().splitlines()\n\n");
        script.append("    for i in range(0, len(hosts), thread_count):\n");
        script.append("        threadList = []\n");
        script.append("        for host in hosts[i : i + thread_count]:\n");
        script.append("            thread = threading.Thread(target=PoC, args=(host,))\n");
        script.append("            threadList.append(thread)\n");
        script.append("            thread.start()\n\n");
        script.append("        for thread in threadList:\n");
        script.append("            thread.join()\n\n");

        // 添加主函数
        script.append("if __name__ == \"__main__\":\n");
        script.append("    parser = argparse.ArgumentParser()\n");
        script.append("    parser.description = \"~~POC description~~\"\n");
        script.append("    parser.add_argument(\"-u\", \"--url\", help=\"Specify URL\", type=str)\n");
        script.append("    parser.add_argument(\"-f\", \"--file\", help=\"Specify the host file\", type=str)\n");
        script.append("    parser.add_argument(\"-t\", \"--thread\", help=\"Thread Count\", type=int, default=1)\n\n");
        script.append("    args = parser.parse_args()\n\n");
        script.append("    if args.url:\n");
        script.append("        PoC(args.url)\n");
        script.append("    elif args.file:\n");
        script.append("        process_hosts(args.file, args.thread)\n");

        return script.toString();
    }


}
