package wayoftime.bloodmagic.ritual.types;

import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import wayoftime.bloodmagic.BloodMagic;
import wayoftime.bloodmagic.ritual.AreaDescriptor;
import wayoftime.bloodmagic.ritual.EnumRuneType;
import wayoftime.bloodmagic.ritual.IMasterRitualStone;
import wayoftime.bloodmagic.ritual.Ritual;
import wayoftime.bloodmagic.ritual.RitualComponent;
import wayoftime.bloodmagic.ritual.RitualRegister;

@RitualRegister("ellipsoid")
public class RitualEllipsoid extends Ritual
{
	public static final String SPHEROID_RANGE = "spheroidRange";
	public static final String CHEST_RANGE = "chest";

	private boolean cached = false;
	private BlockPos currentPos; // Offset

	public RitualEllipsoid()
	{
		super("ritualEllipsoid", 0, 20000, "ritual." + BloodMagic.MODID + ".ellipseRitual");
		addBlockRange(SPHEROID_RANGE, new AreaDescriptor.Rectangle(new BlockPos(-10, -10, -10), new BlockPos(11, 11, 11)));
		addBlockRange(CHEST_RANGE, new AreaDescriptor.Rectangle(new BlockPos(0, 1, 0), 1));

		setMaximumVolumeAndDistanceOfRange(SPHEROID_RANGE, 0, 32, 32);
		setMaximumVolumeAndDistanceOfRange(CHEST_RANGE, 1, 3, 3);
	}

	@Override
	public void performRitual(IMasterRitualStone masterRitualStone)
	{
		World world = masterRitualStone.getWorldObj();
		int currentEssence = masterRitualStone.getOwnerNetwork().getCurrentEssence();

		BlockPos masterPos = masterRitualStone.getMasterBlockPos();
		AreaDescriptor chestRange = masterRitualStone.getBlockRange(CHEST_RANGE);
		TileEntity tileInventory = world.getTileEntity(chestRange.getContainedPositions(masterPos).get(0));

		if (currentEssence < getRefreshCost())
		{
			masterRitualStone.getOwnerNetwork().causeNausea();
			return;
		}

		AreaDescriptor sphereRange = masterRitualStone.getBlockRange(SPHEROID_RANGE);
		AxisAlignedBB sphereBB = sphereRange.getAABB(masterPos);
		int minX = (int) (masterPos.getX() - sphereBB.minX);
		int maxX = (int) (sphereBB.maxX - masterPos.getX()) - 1;
		int minY = (int) (masterPos.getY() - sphereBB.minY);
		int maxY = (int) (sphereBB.maxY - masterPos.getY()) - 1;
		int minZ = (int) (masterPos.getZ() - sphereBB.minZ);
		int maxZ = (int) (sphereBB.maxZ - masterPos.getZ()) - 1;

		if (tileInventory != null)
		{
//			System.out.println("Tile");
			if (tileInventory.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).isPresent())
			{
//				System.out.println("Have inv");
				IItemHandler itemHandler = tileInventory.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).resolve().get();

				if (itemHandler.getSlots() <= 0)
				{
					return;
				}

				int blockSlot = -1;
				for (int invSlot = 0; invSlot < itemHandler.getSlots(); invSlot++)
				{
					ItemStack stack = itemHandler.extractItem(invSlot, 1, true);
					if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem))
						continue;

					blockSlot = invSlot;
					break;
				}

//				System.out.println("Block slot: " + blockSlot);

				if (blockSlot == -1)
				{
					return;
				}

				int xR = Math.max(maxX, minX);
				int yR = Math.max(maxY, minY);
				int zR = Math.max(maxZ, minZ);

				int j = -minX;
				int i = -minY;
				int k = -minZ;

				if (currentPos != null)
				{
					j = currentPos.getY();
					i = Math.min(xR, Math.max(-minX, currentPos.getX()));
					k = Math.min(zR, Math.max(-minZ, currentPos.getZ()));
				}
				int checks = 0;
				int maxChecks = 100;

				while (j <= maxY)
				{
					while (i <= maxX)
					{
						while (k <= maxZ)
						{
							checks++;
							if (checks >= maxChecks)
							{
								this.currentPos = new BlockPos(i, j, k);
//								System.out.println(this.currentPos);
								return;
							}

							if (checkIfEllipsoidShell(xR, yR, zR, i, j, k))
							{
								BlockPos newPos = masterPos.add(i, j, k);
//
								if (!world.isAirBlock(newPos))
								{
									k++;
									continue;
								}

								BlockState placeState = Block.getBlockFromItem(itemHandler.getStackInSlot(blockSlot).getItem()).getDefaultState();
								world.setBlockState(newPos, placeState);

								itemHandler.extractItem(blockSlot, 1, false);
								tileInventory.markDirty();
								// TODO:
								masterRitualStone.getOwnerNetwork().syphon(masterRitualStone.ticket(getRefreshCost()));
								k++;
								this.currentPos = new BlockPos(i, j, k);
//								System.out.println(this.currentPos);
								return;
							}
							k++;
						}
						i++;
						k = -minZ;
					}
					j++;
					i = -minX;
					this.currentPos = new BlockPos(i, j, k);
					return;
				}

				j = -minY;
				this.currentPos = new BlockPos(i, j, k);
				return;
			}
		}
	}

	public boolean checkIfEllipsoidShell(int xR, int yR, int zR, int xOff, int yOff, int zOff)
	{
		// Checking shell in the x-direction
		if (!checkIfEllipsoid(xR, yR, zR, xOff, yOff, zOff))
		{
			return false;
		}

		return !((checkIfEllipsoid(xR, yR, zR, xOff + 1, yOff, zOff) && checkIfEllipsoid(xR, yR, zR, xOff - 1, yOff, zOff)) && (checkIfEllipsoid(xR, yR, zR, xOff, yOff + 1, zOff) && checkIfEllipsoid(xR, yR, zR, xOff, yOff - 1, zOff)) && (checkIfEllipsoid(xR, yR, zR, xOff, yOff, zOff + 1) && checkIfEllipsoid(xR, yR, zR, xOff, yOff, zOff - 1)));
	}

	public boolean checkIfEllipsoid(float xR, float yR, float zR, float xOff, float yOff, float zOff)
	{
		float possOffset = 0.5f;
		return xOff * xOff / ((xR + possOffset) * (xR + possOffset)) + yOff * yOff / ((yR + possOffset) * (yR + possOffset)) + zOff * zOff / ((zR + possOffset) * (zR + possOffset)) <= 1;
	}

	@Override
	public int getRefreshCost()
	{
		return 10;// Temporary
	}

	@Override
	public int getRefreshTime()
	{
		return 1;
	}

//    @Override
//    public void readFromNBT(NBTTagCompound tag)
//    {
//        super.readFromNBT(tag);
//        tag
//    }

	@Override
	public void gatherComponents(Consumer<RitualComponent> components)
	{
		addCornerRunes(components, 1, 0, EnumRuneType.DUSK);

		addRune(components, 4, 0, 0, EnumRuneType.FIRE);
		addRune(components, 5, 0, 0, EnumRuneType.FIRE);
		addRune(components, 5, 0, -1, EnumRuneType.FIRE);
		addRune(components, 5, 0, -2, EnumRuneType.FIRE);
		addRune(components, -4, 0, 0, EnumRuneType.FIRE);
		addRune(components, -5, 0, 0, EnumRuneType.FIRE);
		addRune(components, -5, 0, 1, EnumRuneType.FIRE);
		addRune(components, -5, 0, 2, EnumRuneType.FIRE);

		addRune(components, 0, 0, 4, EnumRuneType.AIR);
		addRune(components, 0, 0, 5, EnumRuneType.AIR);
		addRune(components, 1, 0, 5, EnumRuneType.AIR);
		addRune(components, 2, 0, 5, EnumRuneType.AIR);
		addRune(components, 0, 0, -4, EnumRuneType.AIR);
		addRune(components, 0, 0, -5, EnumRuneType.AIR);
		addRune(components, -1, 0, -5, EnumRuneType.AIR);
		addRune(components, -2, 0, -5, EnumRuneType.AIR);

		addRune(components, 3, 0, 1, EnumRuneType.EARTH);
		addRune(components, 3, 0, 2, EnumRuneType.EARTH);
		addRune(components, 3, 0, 3, EnumRuneType.EARTH);
		addRune(components, 2, 0, 3, EnumRuneType.EARTH);
		addRune(components, -3, 0, -1, EnumRuneType.EARTH);
		addRune(components, -3, 0, -2, EnumRuneType.EARTH);
		addRune(components, -3, 0, -3, EnumRuneType.EARTH);
		addRune(components, -2, 0, -3, EnumRuneType.EARTH);

		addRune(components, 1, 0, -3, EnumRuneType.WATER);
		addRune(components, 2, 0, -3, EnumRuneType.WATER);
		addRune(components, 3, 0, -3, EnumRuneType.WATER);
		addRune(components, 3, 0, -2, EnumRuneType.WATER);
		addRune(components, -1, 0, 3, EnumRuneType.WATER);
		addRune(components, -2, 0, 3, EnumRuneType.WATER);
		addRune(components, -3, 0, 3, EnumRuneType.WATER);
		addRune(components, -3, 0, 2, EnumRuneType.WATER);
	}

	@Override
	public Ritual getNewCopy()
	{
		return new RitualEllipsoid();
	}
}
