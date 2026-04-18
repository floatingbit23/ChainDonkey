import requests
import struct

def parse_server_met(url):
    print(f"Descargando {url}...")
    response = requests.get(url)
    if response.status_code != 200:
        print("Error al descargar")
        return []
    
    data = response.content
    if not data:
        return []

    # Estructura básica de server.met
    # 1 byte: version (0xE0)
    # 4 bytes: num_servers
    offset = 1
    num_servers = struct.unpack("<I", data[offset:offset+4])[0]
    offset += 4
    
    servers = []
    print(f"Encontrados {num_servers} servidores.")
    
    for i in range(num_servers):
        try:
            # 4 bytes IP, 2 bytes Port
            ip_bytes = data[offset:offset+4]
            ip = ".".join(map(str, ip_bytes))
            port = struct.unpack("<H", data[offset+4:offset+6])[0]
            offset += 6
            
            # Tags
            num_tags = struct.unpack("<I", data[offset:offset+4])[0]
            offset += 4
            
            for _ in range(num_tags):
                tag_type = data[offset]
                name_len = struct.unpack("<H", data[offset+1:offset+3])[0]
                offset += 3 + name_len
                
                if tag_type == 2: # String
                    val_len = struct.unpack("<H", data[offset:offset+2])[0]
                    offset += 2 + val_len
                elif tag_type == 3: # Uint32
                    offset += 4
                elif tag_type == 8: # Uint8
                    offset += 1
                elif tag_type == 9: # Uint16
                    offset += 2
                # ... otros tipos si existen
            
            servers.append((ip, port))
        except Exception as e:
            break
            
    return servers

if __name__ == "__main__":
    url = "http://emuling.gitlab.io/server.met"
    servers = parse_server_met(url)
    with open("servers.txt", "w") as f:
        for ip, port in servers:
            f.write(f"{ip}:{port}\n")
    print("Servidores guardados en servers.txt")
