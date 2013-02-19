

package com.laytonsmith.core;

import com.laytonsmith.abstraction.*;
import com.laytonsmith.core.constructs.*;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This file is responsible for converting CH objects into server objects, and
 * vice versa
 *
 * @author layton
 */
public class ObjectGenerator {

    private static ObjectGenerator pog = null;

    public static ObjectGenerator GetGenerator() {
        if (pog == null) {
            pog = new ObjectGenerator();
        }
        return pog;
    }

    /**
     * Gets a Location Object, given a MCLocation
     *
     * @param l
     * @return
     */
    public CArray location(MCLocation l) {
        CArray ca = CArray.GetAssociativeArray(Target.UNKNOWN);
        Construct x = new CDouble(l.getX(), Target.UNKNOWN);
        Construct y = new CDouble(l.getY(), Target.UNKNOWN);
        Construct z = new CDouble(l.getZ(), Target.UNKNOWN);
        Construct world = new CString(l.getWorld().getName(), Target.UNKNOWN);
        Construct yaw = new CDouble(l.getYaw(), Target.UNKNOWN);
        Construct pitch = new CDouble(l.getPitch(), Target.UNKNOWN);
        ca.set("0", x, Target.UNKNOWN);
        ca.set("1", y, Target.UNKNOWN);
        ca.set("2", z, Target.UNKNOWN);
        ca.set("3", world, Target.UNKNOWN);
        ca.set("4", yaw, Target.UNKNOWN);
        ca.set("5", pitch, Target.UNKNOWN);
        ca.set("x", x, Target.UNKNOWN);
        ca.set("y", y, Target.UNKNOWN);
        ca.set("z", z, Target.UNKNOWN);
        ca.set("world", world, Target.UNKNOWN);
        ca.set("yaw", yaw, Target.UNKNOWN);
        ca.set("pitch", pitch, Target.UNKNOWN);
        return ca;
    }

    /**
     * Given a Location Object, returns a MCLocation. If the optional world is
     * not specified in the object, the world provided is used instead. Location
     * "objects" are MethodScript arrays that represent a location in game. There are
     * 4 usages: <ul> <li>(x, y, z)</li> <li>(x, y, z, world)</li> <li>(x, y, z,
     * yaw, pitch)</li> <li>(x, y, z, world, yaw, pitch)</li> </ul> In all
     * cases, the pitch and yaw default to 0, and the world defaults to the
     * specified world. <em>More conveniently: ([world], x, y, z, [yaw,
     * pitch])</em>
     */
    public MCLocation location(Construct c, MCWorld w, Target t) {
        if (!(c instanceof CArray)) {
            throw new ConfigRuntimeException("Expecting an array, received " + c.getClass().getSimpleName(), ExceptionType.FormatException, t);
        }
        CArray array = (CArray) c;
        MCWorld world = w;
        double x = 0;
        double y = 0;
        double z = 0;
        float yaw = 0;
        float pitch = 0;
        if (!array.inAssociativeMode()) {
			//Just the xyz, with default yaw and pitch, and given world
			x = array.get(0, t).primitive(t).castToDouble(t);
			y = array.get(1, t).primitive(t).castToDouble(t);
			z = array.get(2, t).primitive(t).castToDouble(t);
            if (array.size() == 4) {
                //x, y, z, world
                world = Static.getServer().getWorld(array.get(3, t).val());
            } else if (array.size() == 5) {
                //x, y, z, yaw, pitch, with given world
                yaw = array.get(3, t).primitive(t).castToDouble32(t);
                pitch = array.get(4, t).primitive(t).castToDouble32(t);
            } else if (array.size() == 6) {
                //All have been given
                world = Static.getServer().getWorld(array.get(3, t).val());
                yaw = array.get(4, t).primitive(t).castToDouble32(t);
                pitch = array.get(5, t).primitive(t).castToDouble32(t);
            } else {
                throw new ConfigRuntimeException("Expecting a Location array, but the array did not meet the format specifications", ExceptionType.FormatException, t);
            }
        }
        if (array.containsKey("x")) {
            x = array.get("x").primitive(t).castToDouble(t);
        }
        if (array.containsKey("y")) {
            y = array.get("y").primitive(t).castToDouble(t);
        }
        if (array.containsKey("z")) {
            z = array.get("z").primitive(t).castToDouble(t);
        }
        if (array.containsKey("world")) {
            world = Static.getServer().getWorld(array.get("world").val());
        }
        if (array.containsKey("yaw")) {
            yaw = array.get("yaw").primitive(t).castToDouble32(t);
        }
        if (array.containsKey("pitch")) {
            pitch = array.get("pitch").primitive(t).castToDouble32(t);
        }
		//If world is still null at this point, it's an error
		if (world == null) {
			throw new ConfigRuntimeException("The specified world doesn't exist, or no world was provided", ExceptionType.InvalidWorldException, t);
		}
        return StaticLayer.GetLocation(world, x, y, z, yaw, pitch);
    }

    /**
     * An Item Object consists of data about a particular item stack.
     * Information included is: type, data, qty, and an array of enchantment
     * objects (labeled enchants): etype (enchantment type) and elevel
     * (enchantment level). For backwards compatibility, this information is
     * also listed in numerical slots as well as associative slots. If the
     * MCItemStack is null, or the underlying item is nonexistant (or air) CNull
     * is returned.
     *
     * @param is
     * @return
     */
    public Construct item(MCItemStack is, Target t) {
        if (is == null || is.getAmount() == 0) {
            return Construct.GetNullConstruct(t);
        }
        int type = is.getTypeId();
        
        int data;
        if(type < 256){
            //Use the data
            data = (is.getData() != null ? is.getData().getData() : 0);            
        } else {
            //Use the durability
            data = is.getDurability();
        }
        int qty = is.getAmount();
        CArray enchants = new CArray(t);
        for (Map.Entry<MCEnchantment, Integer> entry : is.getEnchantments().entrySet()) {
            CArray enchObj = CArray.GetAssociativeArray(t);
            enchObj.set("etype", new CString(entry.getKey().getName(), t), t);
            enchObj.set("elevel", new CInt(entry.getValue(), t), t);
            enchants.push(enchObj);
        }
		Construct meta = itemMeta(is, t);
        CArray ret = CArray.GetAssociativeArray(t);
        ret.set("type", Integer.toString(type));
        ret.set("data", Integer.toString(data));
        ret.set("qty", Integer.toString(qty));
        ret.set("enchants", enchants, t);
		ret.set("meta", meta, t);
        return ret;
    }

    /**
     * Gets an MCItemStack from a given item "object". Supports both the old and
     * new formats currently
     *
     * @param i
     * @param line_num
     * @param f
     * @return
     */
    public MCItemStack item(Construct i, Target t) {
        if (i.isNull()) {
            return EmptyItem();
        }
        if (!(i instanceof CArray)) {
            throw new ConfigRuntimeException("Expected an array!", ExceptionType.FormatException, t);
        }
        CArray item = (CArray) i;
        int type = 0;
        int data = 0;
        int qty = 1;
        Map<MCEnchantment, Integer> enchants = new HashMap<MCEnchantment, Integer>();
		MCItemMeta meta = null;

        if (item.containsKey("type")) {
            try {
                if (item.get("type").val().contains(":")) {
                    //We're using the combo addressing method
                    String[] split = item.get("type").val().split(":");
                    item.set("type", split[0]);
                    item.set("data", split[1]);
                }
                type = Integer.parseInt(item.get("type").val());
            } catch (NumberFormatException e) {
                throw new ConfigRuntimeException("Could not get item information from given information (" + item.get("type").val() + ")", ExceptionType.FormatException, t, e);
            }
        } else {
            throw new ConfigRuntimeException("Could not find item type!", ExceptionType.FormatException, t);
        }
        if (item.containsKey("data")) {
            try {
                data = Integer.parseInt(item.get("data").val());
            } catch (NumberFormatException e) {
                throw new ConfigRuntimeException("Could not get item data from given information (" + item.get("data").val() + ")", ExceptionType.FormatException, t, e);
            }
        }
        if (item.containsKey("qty")) {
            //This is the qty
            String sqty = "notanumber";
            if (item.containsKey("qty")) {
                sqty = item.get("qty").val();
            }
            try {
                qty = Integer.parseInt(sqty);
            } catch (NumberFormatException e) {
                throw new ConfigRuntimeException("Could not get qty from given information (" + sqty + ")", ExceptionType.FormatException, t, e);
            }
        }

        if (item.containsKey("enchants")) {
            CArray enchantArray = null;
            try {
                if (item.containsKey("enchants")) {
                    enchantArray = (CArray) item.get("enchants");
                }
                if (enchantArray == null) {
                    throw new NullPointerException();
                }
            } catch (Exception e) {
                throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, t, e);
            }

            for (String index : enchantArray.keySet()) {
                try {
                    CArray enchantment = (CArray) enchantArray.get(index);
                    String setype = null;
                    String selevel = null;
                    if (enchantment.containsKey("etype")) {
                        setype = enchantment.get("etype").val();
                    }

                    if (enchantment.containsKey("elevel")) {
                        selevel = enchantment.get("elevel").val();
                    }
                    if (setype == null || selevel == null) {
                        throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, t);
                    }
                    int elevel = 0;
                    try {
                        elevel = Integer.parseInt(selevel);
                    } catch (NumberFormatException e) {
                        throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, t);
                    }
                    MCEnchantment etype = StaticLayer.GetEnchantmentByName(setype);
                    enchants.put(etype, elevel);
                } catch (ClassCastException e) {
                    throw new ConfigRuntimeException("Could not get enchantment data from given information.", ExceptionType.FormatException, t, e);
                }
            }
        }
		if (item.containsKey("meta")) {
			meta = itemMeta(item.get("meta"), type, t);
		}
        MCItemStack ret = StaticLayer.GetItemStack(type, qty);
        ret.setData(data);
        ret.setDurability((short) data);
		if (meta != null) {
			ret.setItemMeta(meta);
		}
		for (Map.Entry<MCEnchantment, Integer> entry : enchants.entrySet()) {
			ret.addUnsafeEnchantment(entry.getKey(), entry.getValue());
		}

        //Giving them air crashes the client, so just clear the inventory slot
        if (ret.getTypeId() == 0) {
            ret = EmptyItem();
        }
        return ret;
    }

    private static MCItemStack EmptyItem() {
        return StaticLayer.GetItemStack(0, 1);
    }
    
	public Construct itemMeta(MCItemStack is, Target t) {
		Construct ret, display, lore, color, title, author, pages, owner, stored;
		if (!is.hasItemMeta()) {
			ret = Construct.GetNullConstruct(t);
		} else {
			ret = CArray.GetAssociativeArray(t);
			MCItemMeta meta = is.getItemMeta();
			if (meta.hasDisplayName()) {
				display = new CString(meta.getDisplayName(), t);
			} else {
				display = Construct.GetNullConstruct(t);
			}
			if (meta.hasLore()) {
				lore = new CArray(t);
				for (String l : meta.getLore()) {
					((CArray) lore).push(new CString(l, t));
				}
			} else {
				lore = Construct.GetNullConstruct(t);
			}
			((CArray) ret).set("display", display, t);
			((CArray) ret).set("lore", lore, t);
			if (meta instanceof MCLeatherArmorMeta) {
				color = color(((MCLeatherArmorMeta) meta).getColor(), t);
				((CArray) ret).set("color", color, t);
			}
			if (meta instanceof MCBookMeta) {
				if (((MCBookMeta) meta).hasTitle()) {
					title = new CString(((MCBookMeta) meta).getTitle(), t);
				} else {
					title = Construct.GetNullConstruct(t);
				}
				if (((MCBookMeta) meta).hasAuthor()) {
					author = new CString(((MCBookMeta) meta).getAuthor(), t);
				} else {
					author = Construct.GetNullConstruct(t);
				}
				if (((MCBookMeta) meta).hasPages()) {
					pages = new CArray(t);
					for (String p : ((MCBookMeta) meta).getPages()) {
						((CArray) pages).push(new CString(p, t));
					}
				} else {
					pages = Construct.GetNullConstruct(t);
				}
				((CArray) ret).set("title", title, t);
				((CArray) ret).set("author", author, t);
				((CArray) ret).set("pages", pages, t);
			}
			if (meta instanceof MCSkullMeta) {
				if (((MCSkullMeta) meta).hasOwner()) {
					owner = new CString(((MCSkullMeta) meta).getOwner(), t);
				} else {
					owner = Construct.GetNullConstruct(t);
				}
				((CArray) ret).set("owner", owner, t);
			}
			if (meta instanceof MCEnchantmentStorageMeta) {
				if (((MCEnchantmentStorageMeta) meta).hasStoredEnchants()) {
					stored = new CArray(t);
					for (Map.Entry<MCEnchantment, Integer> entry : ((MCEnchantmentStorageMeta) meta).getStoredEnchants().entrySet()) {
						CArray eObj = CArray.GetAssociativeArray(t);
						eObj.set("etype", new CString(entry.getKey().getName(), t), t);
						eObj.set("elevel", new CInt(entry.getValue(), t), t);
						((CArray) stored).push(eObj);
					}
				} else {
					stored = Construct.GetNullConstruct(t);
				}
				((CArray) ret).set("stored", stored, t);
			}
		}
		return ret;
	}
	
	public MCItemMeta itemMeta(Construct c, int i, Target t) {
		if (c == null || c.isNull()) {
			return null;
		}
		MCItemMeta meta = Static.getServer().getItemFactory().getItemMeta(StaticLayer.GetConvertor().getMaterial(i));
		CArray ma;
		if (c instanceof CArray) {
			ma = (CArray) c;
			try {
				if (ma.containsKey("display")) {
					Construct dni = ma.get("display");
					if (!dni.isNull()) {
						meta.setDisplayName(dni.val());
					}
				}
				if (ma.containsKey("lore")) {
					Construct li = ma.get("lore");
					if (li.isNull()) {
						//do nothing
					} else if (li instanceof CArray) {
						CArray la = (CArray) li;
						List<String> ll = new ArrayList<String>();
						for (int j = 0; j < la.size(); j++) {
							ll.add(la.get(j).val());
						}
						meta.setLore(ll);
					} else {
						throw new Exceptions.FormatException("Lore was expected to be an array.", t);
					}
				}
				if (meta instanceof MCLeatherArmorMeta) {
					if (ma.containsKey("color")) {
						Construct ci = ma.get("color");
						if (ci.isNull()) {
							//nothing
						} else if (ci instanceof CArray) {
							((MCLeatherArmorMeta) meta).setColor(color((CArray) ci, t));
						} else {
							throw new Exceptions.FormatException("Color was expected to be an array.", t);
						}
					}
				}
				if (meta instanceof MCBookMeta) {
					if (ma.containsKey("title")) {
						Construct title = ma.get("title");
						if (!title.isNull()) {
							((MCBookMeta) meta).setTitle(title.val());
						}
					}
					if (ma.containsKey("author")) {
						Construct author = ma.get("author");
						if (!author.isNull()) {
							((MCBookMeta) meta).setAuthor(author.val());
						}
					}
					if (ma.containsKey("pages")) {
						Construct pages = ma.get("pages");
						if (pages.isNull()) {
							//nothing
						} else if (pages instanceof CArray) {
							CArray pa = (CArray) pages;
							List<String> pl = new ArrayList<String>();
							for (int j = 0; j < pa.size(); j++) {
								pl.add(pa.get(j).val());
							}
							((MCBookMeta) meta).setPages(pl);
						} else {
							throw new Exceptions.FormatException("Pages field was expected to be an array.", t);
						}
					}
				}
				if (meta instanceof MCSkullMeta) {
					if (ma.containsKey("owner")) {
						Construct owner = ma.get("owner");
						if (!owner.isNull()) {
							((MCSkullMeta) meta).setOwner(owner.val());
						}
					}
				}
				if (meta instanceof MCEnchantmentStorageMeta) {
					if (ma.containsKey("stored")) {
						Construct stored = ma.get("stored");
						if (stored == null || stored.isNull()) {
							//Still doing nothing
						} else if (stored instanceof CArray) {
							for (String index : ((CArray) stored).keySet()) {
								try {
									CArray earray = (CArray) ((CArray) stored).get(index);
									MCEnchantment etype = StaticLayer.GetConvertor().GetEnchantmentByName(earray.get("etype").val());
									int elevel = earray.get("elevel").primitive(t).castToInt32(t);
									((MCEnchantmentStorageMeta) meta).addStoredEnchant(etype, elevel, true);
								} catch (Exception bade) {
									throw new Exceptions.FormatException("Could not get enchantment data from index " + index, t);
								}
							}
						} else {
							throw new Exceptions.FormatException("Stored field was expected to be an array of Enchantment arrays", t);
						}
					}
				}
			} catch(Exception ex) {
				throw new Exceptions.FormatException("Could not get ItemMeta from the given information.", t);
			}
		} else {
			throw new Exceptions.FormatException("An array was expected but recieved " + c + " instead.", t);
		}
		return meta;
	}

    public CArray exception(ConfigRuntimeException e, Target t) {
		CArray ex = new CArray(t);
		ex.push(new CString(e.getExceptionType().toString(), t));
		ex.push(new CString(e.getMessage(), t));
		ex.push(new CString((e.getFile() != null ? e.getFile().getAbsolutePath() : "null"), t));
		ex.push(new CInt(e.getLineNum(), t));
		return ex;
    }
	
	/**
	 * Returns a CArray given an MCColor. It will be in the format
	 * array(r: 0, g: 0, b: 0)
	 * @param color
	 * @param t
	 * @return 
	 */
	public CArray color(MCColor color, Target t){
		CArray ca = new CArray(t);
		ca.set("r", new CInt(color.getRed(), t), t);
		ca.set("g", new CInt(color.getGreen(), t), t);
		ca.set("b", new CInt(color.getBlue(), t), t);
		return ca;
	}
	
	/**
	 * Returns an MCColor given a colorArray, which supports the following
	 * three format types (in this order of priority)
	 * array(r: 0, g: 0, b: 0)
	 * array(red: 0, green: 0, blue: 0)
	 * array(0, 0, 0)
	 * @param color
	 * @param t
	 * @return 
	 */
	public MCColor color(CArray color, Target t){
		int red;
		int green;
		int blue;
		if(color.containsKey("r")){
			red = color.get("r").primitive(t).castToInt32(t);
		} else if(color.containsKey("red")){
			red = color.get("red").primitive(t).castToInt32(t);
		} else {
			red = color.get(0).primitive(t).castToInt32(t);
		}
		if(color.containsKey("g")){
			green = color.get("g").primitive(t).castToInt32(t);
		} else if(color.containsKey("green")){
			green = color.get("green").primitive(t).castToInt32(t);
		} else {
			green = color.get(1).primitive(t).castToInt32(t);
		}
		if(color.containsKey("b")){
			blue = color.get("b").primitive(t).castToInt32(t);
		} else if(color.containsKey("blue")){
			blue = color.get("blue").primitive(t).castToInt32(t);
		} else {
			blue = color.get(2).primitive(t).castToInt32(t);
		}
		return StaticLayer.GetConvertor().GetColor(red, green, blue);
	}
	
	public CArray velocity(MCEntity.Velocity v, Target t) {
		double x,y,z,mag;
		x = y = z = mag = 0;
		if (v != null) {
			x = v.x;
			y = v.y;
			z = v.z;
			mag = v.magnitude;
		}
		CArray ret = CArray.GetAssociativeArray(t);
		ret.set("magnitude", new CDouble(mag, t), t);
		ret.set("x", new CDouble(x, t), t);
		ret.set("y", new CDouble(y, t), t);
		ret.set("z", new CDouble(z, t), t);
		return ret;
	}

	public MCEntity.Velocity velocity(Construct c, Target t) {
		CArray va;
		double x, y, z, mag;
		x = y = z = mag = 0;
		if (c instanceof CArray) {
			va = (CArray) c;
			if (va.containsKey("x")) {
				x = Static.getDouble(va.get("x"), t);
			}
			if (va.containsKey("y")) {
				y = Static.getDouble(va.get("y"), t);
			}
			if (va.containsKey("z")) {
				z = Static.getDouble(va.get("z"), t);
			}
			if (!va.containsKey("x") && !va.containsKey("y") && !va.containsKey("z")) {
				switch ((int) va.size()) {
				case 4:
					z = Static.getDouble(va.get(3), t);
					y = Static.getDouble(va.get(2), t);
					x = Static.getDouble(va.get(1), t);
					break;
				case 3:
					z = Static.getDouble(va.get(2), t);
				case 2:
					y = Static.getDouble(va.get(1), t);
				case 1:
					x = Static.getDouble(va.get(0), t);
				}
			}
			return new MCEntity.Velocity(mag, x, y, z);
		} else {
			throw new Exceptions.FormatException("Expected an array but recieved " + c, t);
		}
	}
}
