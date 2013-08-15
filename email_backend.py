#!/usr/bin/env python
# -*- coding: UTF-8 -*-
# vim: ts=4 sts=4 sw=4 tw=79 sta et
"""
Python source code - replace this with a description of the code and write the code below this text.
"""

__author__ = 'Patrick Butler'
__email__  = 'pbutler@killertux.org'

import re
import mailbox
#import yapgvb
import socket
import json
import hashlib


def obfu(email):
    try:
        name, domain = email.split("@")
        domain = domain.split(".")
        last = domain[-1]
        domain = [hashlib.md5(sub).hexdigest()[:len(sub)] for sub in domain]
        domain[-1] = last
        domain = ".".join(domain)
        return hashlib.md5(name).hexdigest()[:5] + "@" + domain
    except Exception, e:
        print e
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


class GraphSender:
    def __init__(self, host = "localhost", port = 4321):
        self.host = host
        self.port = port
        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.s.connect( (host, port) )
        self.edges = {}
        self.nodes = {}

    def send(self, args):
        self.s.send( json.dumps( args , separators=(',',':')) + "\n")

    def addNode(self, label):
        if label == "":
            return -1
        if label not in self.nodes:
            self.nodes[label] = len(self.nodes.keys())
            loc = ""
            if label.find("@") > -1:
                    loc = label.split('@')[1]
            self.send( {"cmd":"na", "name":label, "loc":loc})
        return self.nodes[label]

    def addEdge(self, e1, e2):
        label = ( min(e1, e2), max(e1, e2) )
        if e1 == e2 or e1 == "" or e2 == "":
            return
        if label not in self.edges:
            self.edges[label] = len(self.edges.keys())
            self.send( {"cmd":"ea", "from":self.nodes[label[0]],
            "to":self.nodes[label[1]]} )

    def __del__(self):
        self.send({"cmd":"q"})

def main(args):
    gs = GraphSender()
    print "reading", args[1]
    mbox = mailbox.mbox(args[1])
    coms = {}
    i  = 0
    for msg in mbox:
        i += 1
        frm = cleanup(msg["From"])
        to  = cleanup(msg["To"])
        frm, to = obfu(frm), obfu(to)
        gs.addNode(frm)
        gs.addNode(to)
        gs.addEdge(frm, to)
    print i
    return 0


if __name__ == "__main__":
    import sys
    sys.exit( main( sys.argv ) )

