package com.unknown.emulight.lcp.ui.help;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.unknown.util.ResourceLoader;
import com.unknown.util.log.Levels;
import com.unknown.util.log.Trace;
import com.unknown.xml.dom.Element;
import com.unknown.xml.dom.XMLReader;

public class Help implements TreeModel {
	private static final Logger log = Trace.create(Help.class);

	private static final String TITLE = "Emulight System";
	private final Category root;

	public Help() {
		Category help;
		try {
			help = parseIndex();
		} catch(IOException e) {
			log.log(Levels.ERROR, "Failed to load help index: " + e.getMessage(), e);
			help = new Category(TITLE, null, Collections.emptyList());
		}
		root = help;
	}

	public static InputStream load(String path) {
		return ResourceLoader.loadResource(Help.class, "content/" + path);
	}

	public static URL url(String path) {
		return ResourceLoader.getResource(Help.class, "content/" + path);
	}

	private static Category parseIndex() throws IOException {
		Element rootNode = XMLReader.read(load("index.xml"));
		HelpNode result = parse(rootNode);
		if(result instanceof Category) {
			return (Category) result;
		} else {
			return new Category(TITLE, null, List.of(result));
		}
	}

	private static HelpNode parse(Element xml) {
		String type = xml.getTagName();
		if(type.equals("page")) {
			return new Page(xml.getAttribute("name"), xml.getAttribute("file"));
		} else if(type.equals("category")) {
			String name = xml.getAttribute("name");
			List<HelpNode> result = new ArrayList<>();
			for(Element node : xml.getChildren()) {
				result.add(parse(node));
			}
			String page = xml.getAttribute("page");
			return new Category(name, page, result);
		} else if(type.equals("help")) {
			String name = TITLE;
			List<HelpNode> result = new ArrayList<>();
			for(Element node : xml.getChildren()) {
				result.add(parse(node));
			}
			String page = xml.getAttribute("page");
			return new Category(name, page, result);
		} else {
			return new Category("???", null, Collections.emptyList());
		}
	}

	@Override
	public Object getRoot() {
		return root;
	}

	@Override
	public Object getChild(Object parent, int index) {
		if(parent instanceof Category) {
			Category cat = (Category) parent;
			return cat.getChildren().get(index);
		}
		return null;
	}

	@Override
	public int getChildCount(Object parent) {
		if(parent instanceof Category) {
			Category cat = (Category) parent;
			return cat.getChildren().size();
		} else {
			return 0;
		}
	}

	@Override
	public boolean isLeaf(Object node) {
		return !(node instanceof Category);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// TODO Auto-generated method stub
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if(parent instanceof Category) {
			Category cat = (Category) parent;
			return cat.getChildren().indexOf(child);
		} else {
			return 0;
		}
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		// tree is immutable, listener is useless
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		// tree is immutable, listener is useless
	}
}
