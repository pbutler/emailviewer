# Create your views here.

# Create your views here.
#from django.template import RequestContext, loader
from django.http import HttpResponse
from django.template import RequestContext, loader

import re
import mailbox
import json
import hashlib
import os


def network(request, filename):
    """
    displays page with network viewer
    """
    template = loader.get_template('viz/network.html')
    context = RequestContext(request, {
        'filename': filename
    })
    return HttpResponse(template.render(context))


def list(request):
    """
    prints a list with links to view various mbox files
    """
    files = "\n".join(["<li><a href=\"/viz/network/{0}\">{0}</a></li>".format(file) for file in os.listdir("data")])
    return HttpResponse("""
                        <html>
                        <head><title>Available data files</title></head>
                        <body>
                        <ul>{}</ul>
                        </body>
                        </html>
                        """.format(files))


def data(request, filename):
    """ 
    returns a json package with the network information
    """
    gs = GraphSender()
    mbox = mailbox.mbox(os.path.join("data", filename))
    i = 0
    for msg in mbox:
        i += 1
        frm = cleanup(msg["From"])
        to = cleanup(msg["To"])
        frm, to = obfu(frm), obfu(to)
        gs.addNode(frm)
        gs.addNode(to)
        gs.addEdge(frm, to)

    return HttpResponse(json.dumps(
        {"nodes": sorted(gs.nodes.values(), key=lambda x: x['id']),
         "links": sorted(gs.edges.values(), key=lambda x: x['id'])
        }), mimetype="application/json")


#### Code below reads and parses mbox files into json
def obfu(email):
    try:
        name, domain = email.split("@")
        domain = domain.split(".")
        last = domain[-1]
        domain = [hashlib.md5(sub).hexdigest()[:len(sub)] for sub in domain]
        domain[-1] = last
        domain = ".".join(domain)
        return hashlib.md5(name).hexdigest()[:5] + "@" + domain
    except:
        return hashlib.md5(email).hexdigest()[:10]


def cleanup(email):
    if email is None or email == "":
        return ""
    m = re.search("\s*<?([^<>, ;:'\"]+@[^<>, :;'\"]+)>?,?\s*", email)
    if m is None:
        print "*******", "failed to parse %s" % email
        return ""
        raise Exception("%s failed to parse" % (email))
    val = m.group(1).lower()
    val = val.replace("\"", "")
    val = val.replace("?", "_")
    return val.strip()


class GraphSender(object):
    def __init__(self):
        self.edges = {}
        self.nodes = {}

    def addNode(self, label):
        if label == "":
            return -1
        if label not in self.nodes:
            loc = ""
            if label.find("@") > -1:
                loc = label.split('@')[1]
            self.nodes[label] = {
                'name': label,
                'id': len(self.nodes),
                'loc': loc,
                'count': 0
            }
            #self.send( {"cmd":"na", "name":label, "loc":loc})
        self.nodes[label]['count'] += 1
        return self.nodes[label]

    def addEdge(self, e1, e2):
        label = (min(e1, e2), max(e1, e2))
        if e1 == e2 or e1 == "" or e2 == "":
            return
        if label not in self.edges:
            self.edges[label] = {
                'id': len(self.edges),
                'source': self.nodes[e1]['id'],
                'target': self.nodes[e2]['id'],
                'count': 0
            }
        self.edges[label]['count'] += 1

